package com.parut.product.product.presentation.stock.dto.response;

import com.parut.product.product.application.stock.dto.IsolatedReservationResult;

import java.time.Instant;
import java.util.UUID;

public record IsolatedReservationResponse(
        UUID reservationId,
        UUID productId,
        String productName,
        UUID sellerId,
        UUID orderId,
        int quantity,
        Instant expiresAt
) {
    public static IsolatedReservationResponse from(IsolatedReservationResult result) {
        return new IsolatedReservationResponse(
                result.reservationId(),
                result.productId(),
                result.productName(),
                result.sellerId(),
                result.orderId(),
                result.quantity(),
                result.expiresAt()
        );
    }
}