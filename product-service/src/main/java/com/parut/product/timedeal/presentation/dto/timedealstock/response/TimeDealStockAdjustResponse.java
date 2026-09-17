package com.parut.product.timedeal.presentation.dto.timedealstock.response;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustResult;

import java.util.UUID;

public record TimeDealStockAdjustResponse(
        UUID timeDealId,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity
) {

    public static TimeDealStockAdjustResponse from(TimeDealStockAdjustResult timeDealStockAdjustResult) {
        return new TimeDealStockAdjustResponse(
                timeDealStockAdjustResult.timeDealId(),
                timeDealStockAdjustResult.availableQuantity(),
                timeDealStockAdjustResult.reservedQuantity(),
                timeDealStockAdjustResult.soldQuantity()
        );
    }
}
