package com.parut.order.payment.application.port.in.dto;

import java.time.Instant;

/** Payment가 처리한 실제 결제 취소 결과. */
public record PaymentCancelView(
        long canceledAmount,
        Instant canceledAt
) {
}
