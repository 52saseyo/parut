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
        if (availableQuantity < 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_NEGATIVE_STOCK_QUANTITY);
        }
        this.timeDealId = timeDealId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
        this.soldQuantity = 0;
        this.lowStockThreshold = lowStockThreshold;
    }

    public static TimeDealStock create(TimeDeal timeDeal, Integer availableQuantity, Integer lowStockThreshold) {
        return new TimeDealStock(timeDeal.getId(), availableQuantity, lowStockThreshold);
    }
}