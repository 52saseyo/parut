package com.parut.product.timedeal.application.dto.timedealstock;

import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;

import java.util.UUID;

public record TimeDealStockQueryResult(
        UUID timeDealId,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity,
        Integer lowStockThreshold
) {

    public static TimeDealStockQueryResult from(TimeDealStock timeDealStock) {
        return new TimeDealStockQueryResult(
                timeDealStock.getTimeDealId(),
                timeDealStock.getAvailableQuantity(),
                timeDealStock.getReservedQuantity(),
                timeDealStock.getSoldQuantity(),
                timeDealStock.getLowStockThreshold()
        );
    }
}
