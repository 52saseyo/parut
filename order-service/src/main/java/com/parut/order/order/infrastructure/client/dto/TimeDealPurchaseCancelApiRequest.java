package com.parut.order.order.infrastructure.client.dto;

// TimeDealPurchaseCancelRequest(product-service)와 동일한 필드 구조. reason은 null 허용
public record TimeDealPurchaseCancelApiRequest(
        String reason
) {
}
