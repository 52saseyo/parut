package com.parut.product.timedeal.domain.timedealstock;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_time_deal_stocks")
public class TimeDealStock extends DeletableEntity {

    @Column(name = "time_deal_id", columnDefinition = "uuid", nullable = false, unique = true)
    private UUID timeDealId;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold;

    private TimeDealStock(UUID timeDealId, Integer availableQuantity, Integer lowStockThreshold) {
        validateRequiredFields(timeDealId, availableQuantity, lowStockThreshold);
        validateInitialAvailableQuantity(availableQuantity);
        // availableQuantity == 생성 시점의 초기 재고 (reservedQuantity/soldQuantity가 항상 0으로 시작하므로).
        // 재고 소진에 따라 줄어든 이후의 availableQuantity와 비교하는 용도로 재사용하면 안 됨.
        validateInitialLowStockThreshold(lowStockThreshold, availableQuantity);

        this.timeDealId = timeDealId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
        this.soldQuantity = 0;
        this.lowStockThreshold = lowStockThreshold;
    }

    // NOTE: 애그리거트 간 참조는 ID로만 한다 — TimeDeal 객체를 받지 않는다.
    // 검증에 TimeDeal의 상태를 쓸 일이 없고(생성 시점엔 항상 SCHEDULED), getId()만 필요하기 때문.
    // 따라서 Application Service가 TimeDeal을 먼저 저장해 ID를 확보한 뒤 그 ID를 넘겨야 한다
    // (저장 전 TimeDeal의 id는 null이라 여기서 INVALID_INPUT_VALUE로 걸러진다).
    public static TimeDealStock create(UUID timeDealId, Integer availableQuantity, Integer lowStockThreshold) {
        return new TimeDealStock(timeDealId, availableQuantity, lowStockThreshold);
    }

    // NOTE: TimeDealPurchase.create()는 여기서 하지 않음 — Application Service가 같은 트랜잭션에서 별도 호출해야 함
    // NOTE: 삭제된 재고에서 선점이 일어나면 삭제된 타임딜이 다시 유통되므로 여기서 막는다.
    // 나머지 재고 메서드(confirmSale/cancelReservation/cancelSale)는 이미 선점된 건의 후속 처리라 가드를 두지 않는다.
    public void reserve(Integer quantity) {
        if (isDeleted()) {
            throw new BusinessException(ErrorCode.TIME_DEAL_STOCK_DELETED);
        }
        validateReserveQuantity(quantity);
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    // NOTE: TimeDealPurchase.confirm()은 여기서 하지 않음 — Application Service가 같은 트랜잭션에서 같이 호출해야 함
    public void confirmSale(Integer quantity) {
        validateConfirmSaleQuantity(quantity);
        this.reservedQuantity -= quantity;
        this.soldQuantity += quantity;
    }

    // NOTE: 재고 복구 (reserve의 반대). TimeDealPurchase.cancel()/expire() 호출 시 같은 트랜잭션에서 같이 호출해야 함
    public void cancelReservation(Integer quantity) {
        validateCancelReservationQuantity(quantity);
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    // NOTE: 판매 확정 취소 (confirmSale의 반대). 배송 시작 전 주문 취소 정책상 CONFIRMED 구매도 취소 가능하므로,
    // reservedQuantity가 아닌 availableQuantity로 직접 복구해 재판매 가능 상태로 되돌린다.
    // TimeDealPurchase.cancel() 호출 시 같은 트랜잭션에서 같이 호출해야 함
    public void cancelSale(Integer quantity) {
        validateCancelSaleQuantity(quantity);
        this.soldQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    // 판매자/운영자의 수동 재고 조정. 구매 흐름의 자동 차감(reserve/confirmSale)과는 별개의 행위다 —
    // reserve 계열은 available/reserved/sold 사이를 이동시켜 총합을 보존하지만, 이 메서드는 총 재고 자체를 바꾼다.
    // delta는 부호로 방향을 표현한다(+10 = 물량 추가, -10 = 물량 회수). 절대값 지정을 받지 않는 이유는
    // 판매 중 조회~수정 사이에 선점이 발생하면 절대값이 그 차감분을 덮어써 재고가 공짜로 생기기 때문이다.
    // 조정 대상은 availableQuantity뿐이다 — 이미 선점된 reservedQuantity와 판매 확정된 soldQuantity는
    // 수동 조정으로 절대 변경하지 않으며, 감소분이 잔여 availableQuantity를 넘으면 예외로 막는다.
    // NOTE: ENDED/STOPPED 타임딜의 재고 조정 차단은 Application Service 책임 (여기선 TimeDeal 상태를 알 수 없음)
    public void adjustAvailableQuantity(Integer delta) {
        validateAdjustDelta(delta);
        this.availableQuantity += delta;
    }

    // NOTE: 선점되거나 판매된 수량이 있으면 삭제할 수 없다 — 진행 중인 구매나 판매 이력의 근거가 사라지기 때문.
    // TimeDeal이 SCHEDULED에서만 삭제 가능한 것과 정합적이다(SCHEDULED면 판매가 없었으므로 둘 다 0).
    // 반대로 이 조건만 통과하면 삭제 가능하므로, "TimeDeal과 함께만 삭제한다"는 순서는
    // Application Service가 보장해야 한다(여기서 TimeDeal 상태를 알 수 없음).
    @Override
    public void softDelete(String deletedBy) {
        if (reservedQuantity > 0 || soldQuantity > 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_STOCK_DELETE_NOT_ALLOWED);
        }
        super.softDelete(deletedBy);
    }

    // NOTE: true면 Application Service가 같은 트랜잭션에서 TimeDeal.end()를 호출해야 함 (여기서 직접 호출하지 않음)
    public boolean isDepleted() {
        return availableQuantity == 0;
    }

    private static void validateRequiredFields(
            UUID timeDealId,
            Integer availableQuantity,
            Integer lowStockThreshold
    ) {
        if (timeDealId == null || availableQuantity == null || lowStockThreshold == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validateInitialAvailableQuantity(Integer availableQuantity) {
        if (availableQuantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (availableQuantity < 1) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STOCK_QUANTITY);
        }
    }

    private static void validateInitialLowStockThreshold(Integer lowStockThreshold, Integer initialQuantity) {
        if (lowStockThreshold == null || initialQuantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (lowStockThreshold < 0 || lowStockThreshold > initialQuantity) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_LOW_STOCK_THRESHOLD);
        }
    }

    private void validateReserveQuantity(Integer quantity) {
        if (quantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PURCHASE_QUANTITY);
        }
        if (quantity > availableQuantity) {
            throw new BusinessException(ErrorCode.TIME_DEAL_STOCK_INSUFFICIENT);
        }
    }

    private void validateConfirmSaleQuantity(Integer quantity) {
        if (quantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PURCHASE_QUANTITY);
        }
        if (quantity > reservedQuantity) {
            throw new BusinessException(ErrorCode.TIME_DEAL_NEGATIVE_STOCK_QUANTITY);
        }
    }

    private void validateCancelReservationQuantity(Integer quantity) {
        if (quantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PURCHASE_QUANTITY);
        }
        if (quantity > reservedQuantity) {
            throw new BusinessException(ErrorCode.TIME_DEAL_NEGATIVE_STOCK_QUANTITY);
        }
    }

    private void validateAdjustDelta(Integer delta) {
        if (delta == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (delta == 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STOCK_ADJUST_QUANTITY);
        }
        // 감소 방향만 상한이 있다. 부호를 뒤집어 비교하지 않고 조정 후 값으로 판단하므로
        // reservedQuantity/soldQuantity를 침범하는 조정이 그대로 걸러진다.
        if (availableQuantity + delta < 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_STOCK_INSUFFICIENT);
        }
    }

    private void validateCancelSaleQuantity(Integer quantity) {
        if (quantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PURCHASE_QUANTITY);
        }
        if (quantity > soldQuantity) {
            throw new BusinessException(ErrorCode.TIME_DEAL_NEGATIVE_STOCK_QUANTITY);
        }
    }
}