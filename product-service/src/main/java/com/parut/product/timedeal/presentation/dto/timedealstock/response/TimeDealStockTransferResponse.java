package com.parut.product.timedeal.presentation.dto.timedealstock.response;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockTransferResult;

import java.util.UUID;

public record TimeDealStockTransferResponse(
        UUID timeDealId,
        UUID productId,
        Integer quantity,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity,
        Integer productAvailableQuantity
) {

    public static TimeDealStockTransferResponse from(
            TimeDealStockTransferResult timeDealStockTransferResult
    ) {
        return new TimeDealStockTransferResponse(
                timeDealStockTransferResult.timeDealId(),
                timeDealStockTransferResult.productId(),
                timeDealStockTransferResult.quantity(),
                timeDealStockTransferResult.availableQuantity(),
                timeDealStockTransferResult.reservedQuantity(),
                timeDealStockTransferResult.soldQuantity(),
                timeDealStockTransferResult.productAvailableQuantity()
        );
    }
}
