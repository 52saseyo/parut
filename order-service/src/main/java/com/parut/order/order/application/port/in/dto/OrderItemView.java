package com.parut.order.order.application.port.in.dto;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderItemStatus;

public record OrderItemView(
        UUID orderItemId,
        UUID orderId,
        UUID buyerId,
        UUID sellerId,
        UUID deliveryGroupId,
        OrderItemStatus itemStatus,
        DeliveryGroupStatus groupStatus,
        long unitPrice,
        int quantity,
        Instant confirmedAt
) {
}
