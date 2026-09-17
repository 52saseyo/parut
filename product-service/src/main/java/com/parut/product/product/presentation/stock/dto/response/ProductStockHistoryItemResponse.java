package com.parut.product.product.presentation.stock.dto.response;

import com.parut.product.product.application.stock.dto.ProductStockHistoryItem;

import java.time.Instant;

public record ProductStockHistoryItemResponse(String source,
                                              String eventType,
                                              int quantity,
                                              Instant occurredAt,
                                              String actorId
) {
    public static ProductStockHistoryItemResponse from(ProductStockHistoryItem item) {
        return new ProductStockHistoryItemResponse(
                item.source(),
                item.eventType(),
                item.quantity(),
                item.occurredAt(),
                item.actorId()
        );
    }
}