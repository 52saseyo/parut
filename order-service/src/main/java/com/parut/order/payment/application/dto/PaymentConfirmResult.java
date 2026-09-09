package com.parut.order.payment.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.order.domain.OrderStatus;
import com.parut.order.payment.domain.Payment;
import com.parut.order.payment.domain.PaymentMethod;
import com.parut.order.payment.domain.PaymentStatus;

public record PaymentConfirmResult(
        UUID paymentId,
        UUID orderId,
        String orderNo,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        long totalAmount,
        long balanceAmount,
        Instant approvedAt,
        String receiptUrl,
        OrderStatus orderStatus
) {
    public static PaymentConfirmResult from(Payment payment, UUID orderId, String orderNo, OrderStatus orderStatus) {
        return new PaymentConfirmResult(
                payment.getId(),
                orderId,
                orderNo,
                payment.getPaymentStatus(),
                payment.getPaymentMethod(),
                payment.getTotalAmount(),
                payment.getBalanceAmount(),
                payment.getApprovedAt(),
                payment.getReceiptUrl(),
                orderStatus
        );
    }
}
