package com.parut.order.payment.infrastructure.gateway.dto;

public record TossErrorResponse(
        String code,
        String message
) {
}
