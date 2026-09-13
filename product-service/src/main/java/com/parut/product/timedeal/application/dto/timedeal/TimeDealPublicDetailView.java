package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.time.Instant;
import java.util.UUID;

public record TimeDealPublicDetailView(
        UUID timeDealId,
        UUID productId,
        Long dealPrice,
        Instant startAt,
        Instant endAt,
        Integer maxPurchaseQuantity,
        TimeDealStatus status,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity,
        Integer lowStockThreshold
) {
}
