package com.parut.product.timedeal.application.dto.timedealstock;

import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;

import java.util.UUID;

public record TimeDealStockAdjustResult(
        UUID timeDealId,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity
) {
    public static TimeDealStockAdjustResult from(TimeDealStock timeDealStock) {
        return new TimeDealStockAdjustResult(
                timeDealStock.getTimeDealId(),
                timeDealStock.getAvailableQuantity(),
                timeDealStock.getReservedQuantity(),
                timeDealStock.getSoldQuantity()
        );
    }
}
