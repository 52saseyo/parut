package com.parut.order.order.application.dto;

import com.parut.order.order.domain.*;
import com.parut.order.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCancelResult(
        UUID cancelId,
        UUID orderId,
        Long orderCanceledAmount,
        CancelReasonCode cancelReasonCode,
        CanceledByType canceledByType,
        Long cancelProductAmount,
        Long cancelDeliveryFee,
        Long cancelTotalAmount,
        Boolean refundRequired,
        Instant canceledAt,
        List<CanceledItem> canceledItems,
        PaymentSummary payment
) {
    public static OrderCancelResult of(
            OrderCancel orderCancel,
            long orderCanceledAmount,
            List<OrderItem> canceledItems,
            PaymentSummary payment
    ) {
        return new OrderCancelResult(
                orderCancel.getId(),
                orderCancel.getOrderId(),
                orderCanceledAmount,
                orderCancel.getCancelReasonCode(),
                orderCancel.getCanceledByType(),
                orderCancel.getCancelProductAmount(),
                orderCancel.getCancelDeliveryFee(),
                orderCancel.getCancelTotalAmount(),
                orderCancel.getRefundRequired(),
                orderCancel.getCanceledAt(),
                canceledItems.stream().map(CanceledItem::from).toList(),
                payment
        );
    }

    public record CanceledItem(
            UUID orderItemId,
            String productName,
            OrderItemStatus itemStatus
    ) {
        public static CanceledItem from(OrderItem orderItem) {
            return new CanceledItem(orderItem.getId(), orderItem.getProductName(), orderItem.getItemStatus());
        }
    }

    public record PaymentSummary(
            UUID paymentId,
            PaymentStatus paymentStatus,
            Long balanceAmount,
            Long canceledAmount
    ) {
    }
}
