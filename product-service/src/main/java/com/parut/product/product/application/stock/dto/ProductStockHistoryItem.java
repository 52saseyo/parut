package com.parut.product.product.application.stock.dto;

import java.time.Instant;

public record ProductStockHistoryItem(
        String eventType,
        int quantity,
        Instant occurredAt,
        String actorId
) {
}
