package com.parut.order.delivery.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;

public record DeliveryResponse(
        UUID deliveryId,
        UUID deliveryGroupId,
        DeliveryStatus status,
        String trackingNumber,
        Instant shippedAt,
        Instant deliveredAt
) {
    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getDeliveryGroupId(),
                delivery.getStatus(),
                delivery.getTrackingNumber(),
                delivery.getShippedAt(),
                delivery.getDeliveredAt()
        );
    }
}
