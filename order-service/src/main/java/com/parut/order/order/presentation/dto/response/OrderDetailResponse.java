package com.parut.order.order.presentation.dto.response;

import com.parut.order.order.application.dto.OrderDetailData;
import com.parut.order.order.domain.*;
import com.parut.order.payment.domain.PaymentMethod;
import com.parut.order.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID orderId,
        String orderNo,
        OrderType orderType,
        OrderStatus orderStatus,
        Long totalProductAmount,
        Long totalDeliveryFee,
        Long totalPaymentAmount,
        Long canceledAmount,
        Instant expiresAt,
        Instant orderedAt,
        Instant paidAt,
        RecipientResponse recipient,
        PaymentSummaryResponse payment,
        List<DeliveryGroupResponse> deliveryGroups,
        List<OrderCancelResponse> cancels
) {
    public static OrderDetailResponse from(OrderDetailData data) {
        return new OrderDetailResponse(
                data.orderId(),
                data.orderNo(),
                data.orderType(),
                data.orderStatus(),
                data.totalProductAmount(),
                data.totalDeliveryFee(),
                data.totalPaymentAmount(),
                data.canceledAmount(),
                data.expiresAt(),
                data.orderedAt(),
                data.paidAt(),
                RecipientResponse.from(data.recipient()),
                data.payment() == null ? null : PaymentSummaryResponse.from(data.payment()),
                data.deliveryGroups().stream().map(DeliveryGroupResponse::from).toList(),
                data.cancels().stream().map(OrderCancelResponse::from).toList()
        );
    }

    public record RecipientResponse(
            String recipientName,
            String recipientPhone,
            String zipCode,
            String addressBase,
            String addressDetail,
            String deliveryRequest
    ) {
        static RecipientResponse from(OrderDetailData.Recipient recipient) {
            return new RecipientResponse(
                    recipient.recipientName(),
                    recipient.recipientPhone(),
                    recipient.zipCode(),
                    recipient.addressBase(),
                    recipient.addressDetail(),
                    recipient.deliveryRequest()
            );
        }
    }

    public record PaymentSummaryResponse(
            UUID paymentId,
            PaymentStatus paymentStatus,
            PaymentMethod paymentMethod,
            Long totalAmount,
            Long canceledAmount,
            Instant approvedAt,
            String receiptUrl
    ) {
        static PaymentSummaryResponse from(OrderDetailData.PaymentSummary payment) {
            return new PaymentSummaryResponse(
                    payment.paymentId(),
                    payment.paymentStatus(),
                    payment.paymentMethod(),
                    payment.totalAmount(),
                    payment.canceledAmount(),
                    payment.approvedAt(),
                    payment.receiptUrl()
            );
        }
    }

    public record DeliveryGroupResponse(
            UUID deliveryGroupId,
            UUID sellerId,
            DeliveryGroupStatus groupStatus,
            Long productAmount,
            Long deliveryFee,
            Instant canceledAt,
            List<OrderItemResponse> items
    ) {
        static DeliveryGroupResponse from(OrderDetailData.DeliveryGroup group) {
            return new DeliveryGroupResponse(
                    group.deliveryGroupId(),
                    group.sellerId(),
                    group.groupStatus(),
                    group.productAmount(),
                    group.deliveryFee(),
                    group.canceledAt(),
                    group.items().stream().map(OrderItemResponse::from).toList()
            );
        }
    }

    public record OrderItemResponse(
            UUID orderItemId,
            UUID productId,
            UUID timeDealId,
            String productName,
            String appearanceType,
            String origin,
            LocalDate harvestDate,
            String saleUnit,
            BigDecimal unitQuantity,
            Long originalPrice,
            Long unitPrice,
            Integer quantity,
            OrderItemStatus itemStatus,
            boolean cancelable,
            boolean refundable,
            Instant confirmedAt
    ) {
        static OrderItemResponse from(OrderDetailData.Item item) {
            return new OrderItemResponse(
                    item.orderItemId(),
                    item.productId(),
                    item.timeDealId(),
                    item.productName(),
                    item.appearanceType(),
                    item.origin(),
                    item.harvestDate(),
                    item.saleUnit(),
                    item.unitQuantity(),
                    item.originalPrice(),
                    item.unitPrice(),
                    item.quantity(),
                    item.itemStatus(),
                    item.cancelable(),
                    item.refundable(),
                    item.confirmedAt()
            );
        }
    }

    public record OrderCancelResponse(
            UUID cancelId,
            CancelReasonCode cancelReasonCode,
            String cancelReason,
            CanceledByType canceledByType,
            Long cancelProductAmount,
            Long cancelDeliveryFee,
            Long cancelTotalAmount,
            Boolean refundRequired,
            Instant canceledAt
    ) {
        static OrderCancelResponse from(OrderDetailData.Cancel cancel) {
            return new OrderCancelResponse(
                    cancel.cancelId(),
                    cancel.cancelReasonCode(),
                    cancel.cancelReason(),
                    cancel.canceledByType(),
                    cancel.cancelProductAmount(),
                    cancel.cancelDeliveryFee(),
                    cancel.cancelTotalAmount(),
                    cancel.refundRequired(),
                    cancel.canceledAt()
            );
        }
    }
}
