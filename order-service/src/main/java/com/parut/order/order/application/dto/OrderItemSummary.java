package com.parut.order.order.application.dto;

import com.parut.order.order.domain.*;

import java.time.Instant;
import java.util.UUID;

public record OrderItemSummary(
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
    public static OrderItemSummary from(OrderItem item, Order order, OrderDeliveryGroup group) {
        return new OrderItemSummary(
                item.getId(),
                order.getId(),
                order.getOrderNo(),
                order.getOrderType(),
                order.getOrderStatus(),
                order.getOrderedAt(),
                order.getPaidAt(),
                group.getId(),
                group.getSellerId(),
                group.getGroupStatus(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getItemStatus()
        );
    }
}
