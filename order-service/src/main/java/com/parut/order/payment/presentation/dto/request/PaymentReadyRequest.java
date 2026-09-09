package com.parut.order.payment.presentation.dto.request;

import java.util.UUID;

import com.parut.order.payment.application.dto.PaymentReadyCommand;
import com.parut.order.payment.domain.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record PaymentReadyRequest(
        @NotNull
        UUID orderId,

        @NotNull
        PaymentMethod paymentMethod
) {
    public PaymentReadyCommand toCommand(UUID userId) {
        return new PaymentReadyCommand(orderId, userId, paymentMethod);
    }
}
