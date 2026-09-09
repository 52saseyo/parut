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
// NOTE: 금지 규약 — Application Service는 도메인 메서드를 직접 호출하지 않고 전부 이 클래스를 경유한다.
@Component
public class TimeDealPolicy {


    // NOTE: 타임딜 재고를 선점하고 구매 이력을 생성한다.
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

        // NOTE: 소진 조기 종료. end()에 시간 가드가 없어 호출 경로 제한이 이 클래스의 책임이다.
        if (stock.isDepleted()) {
            timeDeal.end();
        }
        return purchase;
    }

    // NOTE: 결제 완료된 선점을 판매 확정한다. 만료됐으면 정리만 하고 CANCELLED를 반환한다 —
    // 예외를 던지면 그 정리까지 롤백되므로 실패를 반환값으로 표현한다.
    // NOTE: 이미 CONFIRMED면 성공을 그대로 돌려준다(멱등). 이미 CANCELLED인 건은 정리할 것이 없어
    // 반환값을 쓸 이유가 없으므로 도메인의 confirm() 상태 가드가 던지게 둔다.
    public TimeDealPurchaseConfirmResult confirmSale(
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

    // NOTE: 취소 직전 상태에 맞게 재고를 복구한다. ENDED인 타임딜 상태는 복구하지 않아 TimeDeal을 받지 않는다.
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
        Integer quantity = purchase.getQuantity();

        // NOTE: cancel()의 상태 전이 검증이 먼저 돌아, 이미 CANCELLED인 건은 재고를 건드리기 전에 걸린다.
        purchase.cancel(reason);

        if (statusBeforeCancel == TimeDealPurchaseStatus.RESERVED) {
            stock.cancelReservation(quantity);
        } else {
            // NOTE: CONFIRMED는 선점을 거치지 않고 availableQuantity로 바로 복구한다.
            stock.cancelSale(quantity);
        }
    }

    // NOTE: 판매자·운영자의 수동 재고 조정. delta의 부호가 방향(+ 추가, − 회수).
    // ENDED/STOPPED 차단이 여기 있는 이유는 TimeDealStock이 TimeDeal 상태를 알 수 없기 때문이다.
    public void adjustStock(TimeDeal timeDeal, TimeDealStock stock, Integer delta) {
        validateRequiredFields(timeDeal, stock);
        stock.validateBelongsToTimeDeal(timeDeal.getId());

        if (timeDeal.getStatus() == TimeDealStatus.ENDED
                || timeDeal.getStatus() == TimeDealStatus.STOPPED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_STOCK_ADJUST_NOT_ALLOWED);
        }
        stock.adjustAvailableQuantity(delta);
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
        if (timeDeal.getMaxPurchaseQuantity() > initialQuantity) {
            throw new BusinessException(ErrorCode.TIME_DEAL_MAX_PURCHASE_QUANTITY_EXCEEDS_STOCK);
        }
        return TimeDealStock.create(timeDeal.getId(), initialQuantity, lowStockThreshold);
    }

    // NOTE: 타임딜과 재고를 함께 삭제한다. 두 삭제 조건은 서로를 함의하지 않으므로(SCHEDULED여도 선점이
    // 있을 수 있다) 둘 다 검증한 뒤에 변경을 시작한다 — 하나만 바뀐 채로 예외가 나가지 않게.
    public void delete(TimeDeal timeDeal, TimeDealStock stock, String deletedBy) {
        validateRequiredFields(timeDeal, stock);
        stock.validateBelongsToTimeDeal(timeDeal.getId());

        timeDeal.validateDeletable();
        stock.validateDeletable();

        timeDeal.softDelete(deletedBy);
        stock.softDelete(deletedBy);
    }

    // NOTE: 판매 기간 경과로 종료한다(배치 경로). end()에 없는 시간 가드를 여기서 세운다.
    // 운영자의 임의 중단은 end()가 아니라 stop()이며 이 클래스를 경유하지 않는다.
    public void endBySalePeriodEnd(TimeDeal timeDeal, Instant now) {
        if (timeDeal == null || now == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (now.isBefore(timeDeal.getEndAt())) {
            throw new BusinessException(ErrorCode.TIME_DEAL_SALE_PERIOD_NOT_ENDED);
        }
        timeDeal.end();
    }

    private static void validateRequiredFields(Object first, Object second) {
        if (first == null || second == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}