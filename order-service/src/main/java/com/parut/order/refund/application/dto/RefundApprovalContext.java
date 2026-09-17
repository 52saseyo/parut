package com.parut.order.refund.application.dto;

import java.util.List;
import java.util.UUID;

/** Payment 취소 전 검증을 마친 환불 승인 정보. */
public record RefundApprovalContext(
        UUID orderId,
        UUID sellerId,
        List<UUID> refundIds,
        List<UUID> orderItemIds,
        long totalRefundAmount
) {
}
