package com.parut.order.payment.application.dto;

import java.util.UUID;

// 결제 승인 흐름 중 PG 호출들 사이에서 필요한 최소 식별자만 포함 (PaymentFacade 전용)
public record PaymentConfirmContext(
        UUID paymentId,
        UUID orderId,
        UUID userId,
        UUID orderItemId,
        UUID productId
) {
}
