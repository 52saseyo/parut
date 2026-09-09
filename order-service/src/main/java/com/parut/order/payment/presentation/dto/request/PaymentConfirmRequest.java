package com.parut.order.payment.presentation.dto.request;

import com.parut.order.payment.application.dto.PaymentConfirmCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentConfirmRequest(
        @NotBlank
        String paymentKey,

        @NotBlank
        String tossOrderId,

        @NotNull
        @Positive
        Long amount
) {
    public PaymentConfirmCommand toCommand(String idempotencyKey) {
        return new PaymentConfirmCommand(paymentKey, tossOrderId, amount, idempotencyKey);
    }
}
