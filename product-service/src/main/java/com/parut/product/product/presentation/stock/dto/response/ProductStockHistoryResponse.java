package com.parut.product.product.presentation.stock.dto.response;

import com.parut.product.product.application.stock.dto.ProductStockHistoryResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductStockHistoryResponse(
        String productName,
        List<ProductStockHistoryItemResponse> items,
        String nextCursor
) {
    public static ProductStockHistoryResponse from(ProductStockHistoryResult result) {
        return new ProductStockHistoryResponse(
                result.productName(),
                result.items().stream().map(ProductStockHistoryItemResponse::from).toList(),
                result.nextCursor()
        );
    }

}