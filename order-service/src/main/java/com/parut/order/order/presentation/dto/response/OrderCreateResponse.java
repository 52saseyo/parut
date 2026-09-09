package com.parut.order.order.presentation.dto.response;

import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderStatus;
import com.parut.order.order.domain.OrderType;

import java.time.Instant;
import java.util.UUID;

public record OrderCreateResponse(
        UUID orderId,
        String orderNo,
        OrderType orderType,
        OrderStatus orderStatus,
        Long totalProductAmount,
        Long totalDeliveryFee,
        Long totalPaymentAmount,
        Instant expiresAt,
        Instant orderedAt
) {
    public static OrderCreateResponse from(Order order) {
        return new OrderCreateResponse(
                order.getId(),
                order.getOrderNo(),
                order.getOrderType(),
                order.getOrderStatus(),
                order.getTotalProductAmount(),
                order.getTotalDeliveryFee(),
                order.getTotalPaymentAmount(),
                order.getExpiresAt(),
                order.getOrderedAt()
        );
    }
}
