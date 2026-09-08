package com.parut.order.delivery.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;

public record StartDeliveryResponse(
        UUID deliveryId,
        DeliveryStatus previousStatus,
        DeliveryStatus status,
        String trackingNumber,
        Instant shippedAt
) {
    public static StartDeliveryResponse from(Delivery delivery) {
        return new StartDeliveryResponse(
                delivery.getId(),
                DeliveryStatus.PREPARING,
                delivery.getStatus(),
                delivery.getTrackingNumber(),
                delivery.getShippedAt()
        );
    }
}
