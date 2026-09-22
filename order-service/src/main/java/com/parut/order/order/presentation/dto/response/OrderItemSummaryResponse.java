package com.parut.order.order.presentation.dto.response;

import com.parut.order.order.application.dto.OrderItemSummary;
import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.domain.OrderStatus;
import com.parut.order.order.domain.OrderType;

import java.time.Instant;
import java.util.UUID;

public record OrderItemSummaryResponse(
        UUID orderItemId,
        UUID orderId,
        String orderNo,
        OrderType orderType,
        OrderStatus orderStatus,
        Instant orderedAt,
        Instant paidAt,
        UUID deliveryGroupId,
        UUID sellerId,
        DeliveryGroupStatus groupStatus,
        String productName,
        Integer quantity,
        Long unitPrice,
        OrderItemStatus itemStatus
) {
    public static OrderItemSummaryResponse from(OrderItemSummary summary) {
        return new OrderItemSummaryResponse(
                summary.orderItemId(),
                summary.orderId(),
                summary.orderNo(),
                summary.orderType(),
                summary.orderStatus(),
                summary.orderedAt(),
                summary.paidAt(),
                summary.deliveryGroupId(),
                summary.sellerId(),
                summary.groupStatus(),
                summary.productName(),
                summary.quantity(),
                summary.unitPrice(),
                summary.itemStatus()
        );
    }
}
