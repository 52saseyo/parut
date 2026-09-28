package com.parut.order.payment.infrastructure.gateway.dto;

public record TossCancelRequest(
        String cancelReason,
        long cancelAmount
) {
}
