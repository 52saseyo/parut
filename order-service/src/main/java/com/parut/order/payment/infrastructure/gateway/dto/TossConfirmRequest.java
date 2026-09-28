package com.parut.order.payment.infrastructure.gateway.dto;

public record TossConfirmRequest(
        String paymentKey,
        String orderId,
        long amount
) {
}
