package com.parut.order.payment.presentation.dto.request;

// Toss 웹훅 본문. 서명이 없어 여기서 온 값(특히 상태)은 신뢰하지 않고 paymentKey만 재조회에 쓴다.
public record TossWebhookRequest(
        String eventType,
        Data data
) {
    public record Data(
            String paymentKey
    ) {
    }
}
