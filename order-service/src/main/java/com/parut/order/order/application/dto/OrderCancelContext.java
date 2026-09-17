package com.parut.order.order.application.dto;

import java.util.List;
import java.util.UUID;

public record OrderCancelContext(
        UUID orderId,
        long cancelProductAmount,
        long cancelDeliveryFee,
        long cancelTotalAmount,
        List<CancelTargetItem> items
) {
    public record CancelTargetItem(
            UUID orderItemId,
            UUID productId,
            UUID timeDealId
    ) {
    }
}
