package com.parut.order.payment.infrastructure.client.dto;

import java.util.UUID;

// ProductStockConfirmRequest(product-service)와 동일한 필드 구조
public record ProductStockConfirmApiRequest(
        UUID orderId,
        UUID orderItemId
) {
}
