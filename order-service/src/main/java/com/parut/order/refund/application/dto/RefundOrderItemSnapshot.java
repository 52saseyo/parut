package com.parut.order.refund.application.dto;

import java.util.UUID;

/**
 * 환불 요청 검증에 필요한 주문상품 정보.
 */
public record RefundOrderItemSnapshot(
        UUID orderItemId,
        UUID customerId,
        long refundAmount,
        boolean delivered,
        boolean confirmed,
        boolean canceled
) {
}
