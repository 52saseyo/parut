package com.parut.order.order.application.port.in.dto;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderStatus;

public record OrderSnapshotView(
        UUID orderId,
        UUID userId,
        String orderNo,
        OrderStatus orderStatus,
        long totalPaymentAmount,
        Instant expiresAt
) {
    public static OrderSnapshotView from(Order order) {
        return new OrderSnapshotView(
                order.getId(), order.getUserId(), order.getOrderNo(), order.getOrderStatus(),
                order.getTotalPaymentAmount(), order.getExpiresAt()
        );
    }
}
