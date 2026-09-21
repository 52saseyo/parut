package com.parut.order.order.application.port.in.dto;

import java.util.UUID;

public record OrderDeliveryGroupView(
        UUID deliveryGroupId,
        UUID orderId,
        UUID customerId,
        UUID sellerId,
        int shippableItemCount
) {
}
