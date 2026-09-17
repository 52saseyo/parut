package com.parut.order.order.presentation.dto.response;

import com.parut.order.order.application.dto.OrderCancelResult;
import com.parut.order.order.domain.CancelReasonCode;
import com.parut.order.order.domain.CanceledByType;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCancelResponse(
        UUID cancelId,
        UUID orderId,
        Long canceledAmount,
        CancelReasonCode cancelReasonCode,
        CanceledByType canceledByType,
        Long cancelProductAmount,
        Long cancelDeliveryFee,
        Long cancelTotalAmount,
        Boolean refundRequired,
        Instant canceledAt,
        List<CanceledItemResponse> canceledItems,
        PaymentResponse payment
) {
    public static OrderCancelResponse from(OrderCancelResult result) {
        return new OrderCancelResponse(
                result.cancelId(),
                result.orderId(),
                result.orderCanceledAmount(),
                result.cancelReasonCode(),
                result.canceledByType(),
                result.cancelProductAmount(),
                result.cancelDeliveryFee(),
                result.cancelTotalAmount(),
                result.refundRequired(),
                result.canceledAt(),
                result.canceledItems().stream().map(CanceledItemResponse::from).toList(),
                result.payment() == null ? null : PaymentResponse.from(result.payment())
        );
    }

    public record CanceledItemResponse(
            UUID orderItemId,
            String productName,
            OrderItemStatus itemStatus
    ) {
        static CanceledItemResponse from(OrderCancelResult.CanceledItem item) {
            return new CanceledItemResponse(item.orderItemId(), item.productName(), item.itemStatus());
        }
    }

    public record PaymentResponse(
            UUID paymentId,
            PaymentStatus paymentStatus,
            Long balanceAmount,
            Long canceledAmount
    ) {
        static PaymentResponse from(OrderCancelResult.PaymentSummary payment) {
            return new PaymentResponse(
                    payment.paymentId(), payment.paymentStatus(), payment.balanceAmount(), payment.canceledAmount());
        }
    }
}
