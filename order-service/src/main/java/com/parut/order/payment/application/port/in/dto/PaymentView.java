package com.parut.order.payment.application.port.in.dto;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.payment.domain.PaymentMethod;
import com.parut.order.payment.domain.PaymentStatus;

public record PaymentView(
        UUID paymentId,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        Long totalAmount,
        Long balanceAmount,
        Long canceledAmount,
        Instant approvedAt,
        String receiptUrl
) {
}
