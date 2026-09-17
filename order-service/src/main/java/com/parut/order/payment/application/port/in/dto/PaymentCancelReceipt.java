package com.parut.order.payment.application.port.in.dto;

import java.time.Instant;

public record PaymentCancelReceipt(
        long cancelAmount,
        String reason,
        Instant canceledAt,
        String pgTransactionKey
) {
}
