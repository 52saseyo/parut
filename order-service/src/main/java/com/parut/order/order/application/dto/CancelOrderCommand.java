package com.parut.order.order.application.dto;

import java.util.List;
import java.util.UUID;

import com.parut.order.order.domain.CancelReasonCode;
import com.parut.order.order.domain.CanceledByType;

public record CancelOrderCommand(
        UUID orderId,
        List<UUID> orderItemIds,
        CancelReasonCode cancelReasonCode,
        String cancelReason,
        CanceledByType canceledByType,
        UUID requesterId,
        String idempotencyKey
) {
}
