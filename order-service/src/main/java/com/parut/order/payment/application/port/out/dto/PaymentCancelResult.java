package com.parut.order.payment.application.port.out.dto;

import java.time.Instant;

public record PaymentCancelResult(
        Instant canceledAt,
        String pgTransactionKey
) {
}
