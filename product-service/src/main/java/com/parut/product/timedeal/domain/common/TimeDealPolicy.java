package com.parut.product.timedeal.domain.common;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

// NOTE: 타임딜 컨텍스트의 애그리거트 간 조율을 담당하는 도메인 서비스. 진입점마다 순서를 다시 쓰면
// 한 곳만 놓쳐도 그 경로의 불변식이 깨지므로 조율을 여기 한 곳에 모은다.
// NOTE: 여러 애그리거트의 조율은 이 클래스를 경유한다. 단일 타임딜의 기간 갱신은 엔티티가 담당한다.
@Component
public class TimeDealPolicy {


    // NOTE: 타임딜 재고를 선점하고 구매 이력을 생성한다.
    public void validateReservation(
            TimeDeal timeDeal,
            TimeDealStock stock,
            Integer quantity,
            Integer alreadyPurchasedQuantity,
            Instant now
    ) {
        validateRequiredFields(timeDeal, stock);
        stock.validateBelongsToTimeDeal(timeDeal.getId());
        timeDeal.validatePurchasable(now);
        timeDeal.validatePurchaseQuantity(quantity, alreadyPurchasedQuantity);
    }

    // NOTE: Redis 선점 성공 이후 DB 재고 상태와 구매 이력을 반영하는 기존 경로다.
    // Redis 선점 자체는 Application Port가 담당하고, 이 메서드는 DB 애그리거트 변경만 담당한다.
    public TimeDealPurchase reserve(
            TimeDeal timeDeal,
            TimeDealStock stock,
            UUID orderId,
            UUID userId,
            Integer quantity,
            Integer alreadyPurchasedQuantity,
            Instant now
    ) {
        validateRequiredFields(timeDeal, stock);
        stock.validateBelongsToTimeDeal(timeDeal.getId());

        TimeDealPurchase purchase = TimeDealPurchase.create(timeDeal, orderId, userId, quantity, now);

        // NOTE: create()가 validatePurchasable을 수행하므로 그것이 먼저 걸리도록 이 순서를 유지한다.
        timeDeal.validatePurchaseQuantity(quantity, alreadyPurchasedQuantity);

        stock.reserve(quantity);

        // NOTE: 선점만으로는 결제 확정 여부를 알 수 있으므로 타임딜을 종료하지 않는다.
        //       재고가 0이어도 RESERVED 구매가 취소·만료되면 재고가 복구될 수 있다.
        return purchase;
    }

    // NOTE: Redis 선점 이후 DB 재고를 원자적 UPDATE로 반영하는 경로에서는 stock.reserve()를 호출하지 않는다.
    // DB 재고 변경은 TimeDealStockRepository의 조건부 UPDATE가 담당하고, 이 메서드는 구매 객체만 생성한다.
    public TimeDealPurchase createReservedPurchase(
            TimeDeal timeDeal,
            TimeDealStock stock,
            UUID orderId,
            UUID userId,
            Integer quantity,
            Integer alreadyPurchasedQuantity,
            Instant now
    ) {
        validateRequiredFields(timeDeal, stock);
        stock.validateBelongsToTimeDeal(timeDeal.getId());
        timeDeal.validatePurchaseQuantity(quantity, alreadyPurchasedQuantity);
        return TimeDealPurchase.create(timeDeal, orderId, userId, quantity, now);
    }

    // NOTE: 결제 완료된 선점을 판매 확정한다. 만료됐으면 정리만 하고 CANCELLED를 반환한다 —
    // 예외를 던지면 그 정리까지 롤백되므로 실패를 반환값으로 표현한다.
    // NOTE: 이미 CONFIRMED면 성공을 그대로 돌려준다(멱등). 이미 CANCELLED인 건은 정리할 것이 없어
    // 반환값을 쓸 이유가 없으므로 도메인의 confirm() 상태 가드가 던지게 둔다.
    // NOTE: 확정 시점에는 TimeDeal까지 함께 받아, 모든 선점이 확정된 경우에만 소진 종료한다.
    public TimeDealPurchaseConfirmResult confirmSale(
            TimeDeal timeDeal,
            TimeDealPurchase purchase,
            TimeDealStock stock,
            Instant now
    ) {
        validateRequiredFields(purchase, stock);
        stock.validateBelongsToTimeDeal(purchase.getTimeDealId());

        // NOTE: 재시도로 같은 확정이 또 들어온 경우다(멱등) — 재고를 또 옮기지 않고 원래 결과를 돌려준다.
        if (purchase.getStatus() == TimeDealPurchaseStatus.CONFIRMED) {
            return TimeDealPurchaseConfirmResult.CONFIRMED;
        }

        // NOTE: 수량은 반드시 purchase에서 가져온다. 호출자가 넘긴 값을 쓰면 두 애그리거트가 어긋난다.
        Integer quantity = purchase.getQuantity();

        if (purchase.isExpired(now)) {
            purchase.expire(now);
            stock.cancelReservation(quantity);
            return TimeDealPurchaseConfirmResult.CANCELLED;
        }

        purchase.confirm(now);
        stock.confirmSale(quantity);

        // NOTE: available=0이어도 아직 RESERVED가 남아 있으면 취소·만료로 복구될 수 있으므로 종료하지 않는다.
        //       시간 마감·강제 종료가 먼저 처리된 경우에는 이미 종료된 상태를 다시 전이하지 않는다.
        if (timeDeal != null
                && timeDeal.getStatus() == TimeDealStatus.ACTIVE
                && stock.isDepleted()
                && stock.getReservedQuantity() == 0) {
            timeDeal.end();
        }
        return TimeDealPurchaseConfirmResult.CONFIRMED;
    }

    // NOTE: 만료된 선점을 취소하고 재고를 복구한다(배치 경로). 삭제된 타임딜에 남은 선점도 정리 가능해야
    // 하므로 expire()/cancelReservation()에 삭제 가드를 두지 않았다.
    public void expireReservation(TimeDealPurchase purchase, TimeDealStock stock, Instant now) {
        validateRequiredFields(purchase, stock);
        stock.validateBelongsToTimeDeal(purchase.getTimeDealId());

        Integer quantity = purchase.getQuantity();
        purchase.expire(now);
        stock.cancelReservation(quantity);
    }

    // NOTE: RESERVED만 재고를 복구한다. CONFIRMED는 사용된 판매로 간주해 취소해도 재고를 복구하지 않는다.
    // NOTE: reason은 String이며 null도 허용한다 — enum은 타입 제약이 아니라 문구 카탈로그다.
    public void cancelPurchase(
            TimeDealPurchase purchase,
            TimeDealStock stock,
            String reason
    ) {
        validateRequiredFields(purchase, stock);
        stock.validateBelongsToTimeDeal(purchase.getTimeDealId());

        // NOTE: 이미 CANCELLED면 아무것도 하지 않는다(멱등) — 재고를 또 복구하면 이중 복구가 된다.
        // 도메인의 cancel()은 계속 예외를 던지며, 여기서 걸러 아예 부르지 않는다.
        if (purchase.getStatus() == TimeDealPurchaseStatus.CANCELLED) {
            return;
        }

        TimeDealPurchaseStatus statusBeforeCancel = purchase.getStatus();
        // NOTE: cancel()의 상태 전이 검증이 먼저 돌아, 이미 CANCELLED인 건은 재고를 건드리기 전에 걸린다.
        purchase.cancel(reason);

        if (statusBeforeCancel == TimeDealPurchaseStatus.RESERVED) {
            stock.cancelReservation(purchase.getQuantity());
        }
    }

    // NOTE: 판매자·운영자의 수동 재고 조정. delta의 부호가 방향(+ 추가, − 회수).
    // ENDED/STOPPED 차단이 여기 있는 이유는 TimeDealStock이 TimeDeal 상태를 알 수 없기 때문이다.
    public void adjustStock(TimeDeal timeDeal, TimeDealStock stock, Integer delta) {
        validateStockAdjustment(timeDeal, stock, delta);
        stock.adjustAvailableQuantity(delta);
    }

    // NOTE: 일반상품 연동 이관은 일반상품 재고를 먼저 변경하므로, 외부 Port 호출 전에 타임딜 상태를 검증한다.
    public void validateStockAdjustment(TimeDeal timeDeal, TimeDealStock stock, Integer delta) {
        validateRequiredFields(timeDeal, stock);
        stock.validateBelongsToTimeDeal(timeDeal.getId());
        stock.validateAdjustableQuantity(delta);

        if (timeDeal.getStatus() != TimeDealStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_STOCK_ADJUST_NOT_ALLOWED);
        }
    }

    // NOTE: 일반 상품과 타임딜 사이의 재고 이동 중 타임딜 재고 변경을 조율한다.
    public void transferStock(TimeDeal timeDeal, TimeDealStock stock, Integer quantity) {
        adjustStock(timeDeal, stock, quantity);
    }

    // NOTE: 저장된 TimeDeal에 재고를 할당한다 — 저장 전이면 getId()가 null이라 걸러진다.
    // maxPurchaseQuantity <= 초기 재고 검증이 여기 있는 이유는 두 값이 다른 애그리거트에 있기 때문이다.
    public TimeDealStock allocateStock(
            TimeDeal timeDeal,
            Integer initialQuantity,
            Integer lowStockThreshold
    ) {
        if (timeDeal == null || initialQuantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // NOTE: 재고 자체의 불변식을 먼저 태운다 — 수량이 0이면 max 비교가 먼저 걸려 "1인당 최대 구매 수량" 사유가 나가서
        // 실제 원인(수량 0)을 가린다. 생성만 하고 저장하지 않으므로 뒤에서 예외가 나도 남는 변경이 없다.
        TimeDealStock timeDealStock = TimeDealStock.create(timeDeal.getId(), initialQuantity, lowStockThreshold);

        if (timeDeal.getMaxPurchaseQuantity() > initialQuantity) {
            throw new BusinessException(ErrorCode.TIME_DEAL_MAX_PURCHASE_QUANTITY_EXCEEDS_STOCK);
        }
        return timeDealStock;
    }

    // NOTE: 타임딜과 재고를 함께 삭제한다. 두 삭제 조건은 서로를 함의하지 않으므로(SCHEDULED여도 선점이 있을 수 있다) 둘 다 검증한 뒤에 변경을 시작한다 — 하나만 바뀐 채로 예외가 나가지 않게.
    public void delete(TimeDeal timeDeal, TimeDealStock stock, String deletedBy) {
        validateRequiredFields(timeDeal, stock);
        stock.validateBelongsToTimeDeal(timeDeal.getId());

        timeDeal.validateDeletable();
        stock.validateDeletable();

        timeDeal.softDelete(deletedBy);
        stock.softDelete(deletedBy);
    }

    private static void validateRequiredFields(Object first, Object second) {
        if (first == null || second == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
