package com.parut.order.payment.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.order.application.port.in.dto.OrderSnapshotView;
import com.parut.order.payment.domain.Payment;

public record PaymentReadyResult(
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
    public static PaymentReadyResult from(
            OrderSnapshotView order,
            Payment payment,
            String orderName,
            String successUrl,
            String failUrl
    ) {
        return new PaymentReadyResult(
                payment.getId(),
                order.orderNo(),
                orderName,
                order.totalPaymentAmount(),
                // TODO: User Service 내부 API 추가시 수정, 지금은 null 처리
                null,
                successUrl,
                failUrl,
                order.expiresAt(),
                payment.getIdempotencyKey()
        );
    }
}
