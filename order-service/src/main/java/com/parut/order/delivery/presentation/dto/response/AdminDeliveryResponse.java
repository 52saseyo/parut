package com.parut.order.delivery.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;

public record AdminDeliveryResponse(
        UUID deliveryId,
        UUID deliveryGroupId,
        UUID orderId,
        UUID customerId,
        UUID sellerId,
        DeliveryStatus status,
        String trackingNumber,
        Instant shippedAt,
        Instant deliveredAt
) {
    public static AdminDeliveryResponse from(Delivery delivery) {
        return new AdminDeliveryResponse(
                delivery.getId(),
                delivery.getDeliveryGroupId(),
                delivery.getOrderId(),
                delivery.getCustomerId(),
                delivery.getSellerId(),
                delivery.getStatus(),
                delivery.getTrackingNumber(),
                delivery.getShippedAt(),
                delivery.getDeliveredAt()
        );
    }
}
