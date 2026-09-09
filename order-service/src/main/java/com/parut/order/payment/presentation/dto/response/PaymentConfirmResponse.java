package com.parut.order.payment.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.order.domain.OrderStatus;
import com.parut.order.payment.application.dto.PaymentConfirmResult;
import com.parut.order.payment.domain.PaymentMethod;
import com.parut.order.payment.domain.PaymentStatus;

public record PaymentConfirmResponse(
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
    public static PaymentConfirmResponse from(PaymentConfirmResult result) {
        return new PaymentConfirmResponse(
                result.paymentId(),
                result.orderId(),
                result.orderNo(),
                result.paymentStatus(),
                result.paymentMethod(),
                result.totalAmount(),
                result.balanceAmount(),
                result.approvedAt(),
                result.receiptUrl(),
                result.orderStatus()
        );
    }
}
