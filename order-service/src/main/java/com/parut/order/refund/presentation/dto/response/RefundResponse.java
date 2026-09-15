package com.parut.order.refund.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.domain.RefundStatus;

public record RefundResponse(
        UUID refundId,
        UUID orderItemId,
        RefundStatus status,
        long refundAmount,
        String reason,
        String rejectionReason,
        Instant requestedAt,
        Instant canceledAt,
        Instant processedAt,
        UUID processedBy
) {
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getOrderItemId(),
                refund.getStatus(),
                refund.getRefundAmount(),
                refund.getReason(),
                refund.getRejectionReason(),
                refund.getRequestedAt(),
                refund.getCanceledAt(),
                refund.getProcessedAt(),
                refund.getProcessedBy()
        );
    }
}
