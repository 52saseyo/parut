package com.parut.order.payment.infrastructure.gateway.dto;

// 결제 승인 응답 중 도메인에서 실제로 쓰는 필드만 매핑
public record TossPaymentResponse(
        String method,
        String approvedAt,
        long totalAmount,
        long balanceAmount,
        String lastTransactionKey,
        Receipt receipt
) {
    public record Receipt(String url) {
    }
}
