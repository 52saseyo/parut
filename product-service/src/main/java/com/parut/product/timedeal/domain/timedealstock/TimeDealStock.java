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
        // NOTE: 이때의 availableQuantity가 초기 재고다(reserved/sold가 0으로 시작하므로).
        validateInitialLowStockThreshold(lowStockThreshold, availableQuantity);

        this.timeDealId = timeDealId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
        this.soldQuantity = 0;
        this.lowStockThreshold = lowStockThreshold;
    }

    // NOTE: 애그리거트 간 참조는 ID로만 한다. 저장 전 TimeDeal의 id는 null이라 여기서 걸러진다.
    public static TimeDealStock create(UUID timeDealId, Integer availableQuantity, Integer lowStockThreshold) {
        return new TimeDealStock(timeDealId, availableQuantity, lowStockThreshold);
    }

    // NOTE: 삭제된 재고에서 선점이 일어나면 삭제된 타임딜이 다시 유통되므로 여기서만 삭제 가드를 둔다
    // (나머지 재고 메서드는 이미 선점된 건의 후속 처리라 가드가 없다).
    public void reserve(Integer quantity) {
        if (isDeleted()) {
            throw new BusinessException(ErrorCode.TIME_DEAL_STOCK_DELETED);
        }
        validateReserveQuantity(quantity);
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    public void confirmSale(Integer quantity) {
        validateConfirmSaleQuantity(quantity);
        this.reservedQuantity -= quantity;
        this.soldQuantity += quantity;
    }

    // NOTE: 재고 복구 — reserve의 반대.
    public void cancelReservation(Integer quantity) {
        validateCancelReservationQuantity(quantity);
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    // NOTE: 판매 확정 취소. reservedQuantity를 거치지 않고 availableQuantity로 바로 복구해 재판매 가능하게 한다.
    public void cancelSale(Integer quantity) {
        validateCancelSaleQuantity(quantity);
        this.soldQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    // NOTE: 판매자·운영자의 수동 조정. delta의 부호가 방향(+ 추가, − 회수)이며 총 재고 자체가 바뀐다.
    // 절대값을 받지 않는 이유는 조회~수정 사이에 선점이 끼면 그 차감분을 덮어써 재고가 공짜로 생기기 때문이다.
    public void adjustAvailableQuantity(Integer delta) {
        validateAdjustDelta(delta);
        this.availableQuantity += delta;
    }

    // NOTE: 선점·판매된 수량이 있으면 삭제 불가 — 진행 중인 구매나 판매 이력의 근거가 사라진다.
    // "TimeDeal과 함께만 삭제한다"는 순서는 TimeDealPolicy가 보장한다.
    public void validateDeletable() {
        if (reservedQuantity > 0 || soldQuantity > 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_STOCK_DELETE_NOT_ALLOWED);
        }
    }

    @Override
    public void softDelete(String deletedBy) {
        validateDeletable();
        super.softDelete(deletedBy);
    }

    // NOTE: 짝이 안 맞는 애그리거트(타임딜 A + 타임딜 B의 재고)를 거른다.
    // UUID만 받으므로 TimeDeal에 대한 의존이 생기지 않는다.
    public void validateBelongsToTimeDeal(UUID timeDealId) {
        if (timeDealId == null || !this.timeDealId.equals(timeDealId)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_STOCK_MISMATCH);
        }
    }

    // NOTE: true면 TimeDealPolicy가 TimeDeal.end()를 호출한다(여기서 직접 부르지 않는다).
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
        // NOTE: 조정 후 값으로 판단하므로 reserved/sold를 침범하는 조정이 그대로 걸러진다.
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