package com.parut.order.refund.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.domain.RefundStatus;

/** 관리자 목록에서 소유자 식별자를 함께 제공하는 환불 응답이다. */
public record AdminRefundResponse(
        UUID refundId,
        UUID orderItemId,
        UUID customerId,
        UUID sellerId,
        RefundStatus status,
        long refundAmount,
        String reason,
        String rejectionReason,
        Instant requestedAt,
        Instant canceledAt,
        Instant processedAt,
        UUID processedBy
) {
    public static AdminRefundResponse from(Refund refund) {
        return new AdminRefundResponse(
                refund.getId(),
                refund.getOrderItemId(),
                refund.getCustomerId(),
                refund.getSellerId(),
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
