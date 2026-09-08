package com.parut.order.order.application.port.in.dto;

import java.util.UUID;

public record OrderDeliveryGroupView(
        UUID deliveryGroupId,
        UUID sellerId,
        int nonCanceledItemCount
) {
}
