package com.parut.order.order.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderItemStatus;

public record OrderItemConfirmationResponse(
        UUID orderItemId,
        boolean confirmed,
        Instant confirmedAt
) {
    public static OrderItemConfirmationResponse from(OrderItem orderItem) {
        return new OrderItemConfirmationResponse(
                orderItem.getId(),
                orderItem.getItemStatus() == OrderItemStatus.CONFIRMED,
                orderItem.getConfirmedAt()
        );
    }
}
