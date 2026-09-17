package com.parut.product.product.application.stock.dto;

import java.time.Instant;
import java.util.UUID;

public record IsolatedReservationResult(
        UUID reservationId,
        UUID productId,
        String productName,
        UUID sellerId,
        UUID orderId,
        int quantity,
        Instant expiresAt
) {
}