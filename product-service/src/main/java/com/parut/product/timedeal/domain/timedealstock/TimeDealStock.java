package com.parut.product.timedeal.domain.timedealstock;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
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
        validateAvailableQuantity(availableQuantity);
        // availableQuantity == 생성 시점의 초기 재고 (reservedQuantity/soldQuantity가 항상 0으로 시작하므로).
        // 재고 소진에 따라 줄어든 이후의 availableQuantity와 비교하는 용도로 재사용하면 안 됨.
        validateInitialLowStockThreshold(lowStockThreshold, availableQuantity);

        this.timeDealId = timeDealId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
        this.soldQuantity = 0;
        this.lowStockThreshold = lowStockThreshold;
    }

    public static TimeDealStock create(TimeDeal timeDeal, Integer availableQuantity, Integer lowStockThreshold) {
        if (timeDeal == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new TimeDealStock(timeDeal.getId(), availableQuantity, lowStockThreshold);
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

    private static void validateAvailableQuantity(Integer availableQuantity) {
        if (availableQuantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (availableQuantity < 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_NEGATIVE_STOCK_QUANTITY);
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

    public void reserve(Integer quantity) {
        validateReserveQuantity(quantity);
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
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
}