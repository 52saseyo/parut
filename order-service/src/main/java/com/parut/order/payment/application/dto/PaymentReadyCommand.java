package com.parut.order.payment.application.dto;

import java.util.UUID;

import com.parut.order.payment.domain.PaymentMethod;

public record PaymentReadyCommand(
        UUID orderId,
        UUID userId,
        PaymentMethod paymentMethod
) {
}
