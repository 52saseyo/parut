package com.parut.product.timedeal.presentation.dto.timedeal.response;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockQueryResult;

import java.util.UUID;

public record TimeDealStockResponse(
        UUID timeDealId,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity,
        Integer lowStockThreshold
) {

    public static TimeDealStockResponse from(TimeDealStockQueryResult result) {
        return new TimeDealStockResponse(
                result.timeDealId(),
                result.availableQuantity(),
                result.reservedQuantity(),
                result.soldQuantity(),
                result.lowStockThreshold()
        );
    }
}
