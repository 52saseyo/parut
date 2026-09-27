package com.parut.order.payment.infrastructure.gateway.dto;

import java.util.List;

// cancels 배열은 지금까지의 취소 이력 전체이며, 이번 취소 건은 마지막 원소다.
public record TossCancelResponse(
        List<Cancel> cancels
) {
    public record Cancel(
            String transactionKey,
            String canceledAt
    ) {
    }
}
