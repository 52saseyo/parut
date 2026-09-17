package com.parut.product.product.application.stock.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductStockHistoryItem(
        UUID id,
        String source,
        String eventType,
        int quantity,
        Instant occurredAt,
        String actorId
) {
}
