package com.parut.order.payment.application.dto;

public record PaymentConfirmCommand(
        String paymentKey,
        String tossOrderId,
        long amount,
        String idempotencyKey
) {
}
