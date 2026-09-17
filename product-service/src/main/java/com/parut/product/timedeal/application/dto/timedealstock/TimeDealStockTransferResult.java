package com.parut.product.timedeal.application.dto.timedealstock;

import com.parut.product.global.dto.ProductStockTransferResult;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;

import java.util.UUID;

public record TimeDealStockTransferResult(
        UUID timeDealId,
        UUID productId,
        Integer quantity,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity,
        Integer productAvailableQuantity
) {

    public static TimeDealStockTransferResult from(
            TimeDealStock timeDealStock,
            Integer quantity,
            ProductStockTransferResult productStockTransferResult
    ) {
        return new TimeDealStockTransferResult(
                timeDealStock.getTimeDealId(),
                productStockTransferResult.productId(),
                quantity,
                timeDealStock.getAvailableQuantity(),
                timeDealStock.getReservedQuantity(),
                timeDealStock.getSoldQuantity(),
                productStockTransferResult.productAvailableQuantity()
        );
    }
}
