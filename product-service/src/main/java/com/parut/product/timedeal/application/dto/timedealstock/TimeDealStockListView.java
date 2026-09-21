package com.parut.product.timedeal.application.dto.timedealstock;

import java.time.Instant;
import java.util.UUID;

public record TimeDealStockListView(
        UUID timeDealId,
        Instant startAt,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity,
        Integer lowStockThreshold
) {
}
