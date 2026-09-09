package com.parut.order.order.application.port.in.dto;

import java.util.UUID;

import com.parut.order.order.domain.OrderItem;

public record OrderItemSnapshotView(
        UUID orderItemId,
        UUID productId,
        String productName
) {
    public static OrderItemSnapshotView from(OrderItem item) {
        return new OrderItemSnapshotView(item.getId(), item.getProductId(), item.getProductName());
    }
}
