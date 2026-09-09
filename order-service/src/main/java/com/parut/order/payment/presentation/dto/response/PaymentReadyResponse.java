package com.parut.order.payment.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.payment.application.dto.PaymentReadyResult;

public record PaymentReadyResponse(
        UUID paymentId,
        String tossOrderId,
        String orderName,
        long amount,
        String customerName,
        String successUrl,
        String failUrl,
        Instant expiresAt,
        String idempotencyKey
) {
    public static PaymentReadyResponse from(PaymentReadyResult result) {
        return new PaymentReadyResponse(
                result.paymentId(),
                result.tossOrderId(),
                result.orderName(),
                result.amount(),
                result.customerName(),
                result.successUrl(),
                result.failUrl(),
                result.expiresAt(),
                result.idempotencyKey()
        );
    }
}
