package com.parut.order.payment.application.port.out.dto;

import java.time.Instant;

import com.parut.order.payment.domain.PaymentMethod;

public record PaymentApproveResult(
        PaymentMethod paymentMethod,
        Instant approvedAt,
        String receiptUrl,
        String pgTransactionKey
) {
}
