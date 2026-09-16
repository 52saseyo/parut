package com.parut.order.order.application.port.in.dto;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderItemStatus;

/** 주문상품과 해당 주문, 배송 그룹의 환불 및 정산 판단 정보를 전달한다. */
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
