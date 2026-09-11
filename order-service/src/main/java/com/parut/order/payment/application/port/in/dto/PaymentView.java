package com.parut.order.payment.application.port.in.dto;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.payment.domain.Payment;
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
    public static PaymentView from(Payment payment) {
        return new PaymentView(
                payment.getId(),
                payment.getPaymentStatus(),
                payment.getPaymentMethod(),
                payment.getTotalAmount(),
                payment.getBalanceAmount(),
                payment.getCanceledAmount(),
                payment.getApprovedAt(),
                payment.getReceiptUrl()
        );
    }
}
