package com.parut.order.payment.application.port.in.dto;

import java.util.UUID;

import com.parut.order.payment.domain.Payment;
import com.parut.order.payment.domain.PaymentStatus;

public record PaymentCancelView(
        UUID paymentId,
        PaymentStatus paymentStatus,
        Long balanceAmount,
        Long canceledAmount,
        UUID paymentTransactionId
) {
    public static PaymentCancelView of(Payment payment, UUID paymentTransactionId) {
        return new PaymentCancelView(
                payment.getId(),
                payment.getPaymentStatus(),
                payment.getBalanceAmount(),
                payment.getCanceledAmount(),
                paymentTransactionId
        );
    }
}
