package com.parut.order.payment.application.dto;

import java.util.UUID;

public record PaymentConfirmCommand(
        String paymentKey,
        String tossOrderId,
        long amount,
        String idempotencyKey,
        UUID userId
) {
}
