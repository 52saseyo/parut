package com.parut.order.payment.application.port.in.dto;

import java.util.UUID;

/** Payment에 전달할 결제 취소 요청 정보. */
public record PaymentCancelCommand(
        UUID orderId,
        String cancelRequestId,
        long cancelAmount,
        String reason
) {
}
