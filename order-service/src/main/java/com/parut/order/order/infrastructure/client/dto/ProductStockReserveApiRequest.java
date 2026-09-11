package com.parut.order.order.infrastructure.client.dto;

import java.util.UUID;

// ProductStockReserveRequest(product-service)와 동일한 필드 구조
public record ProductStockReserveApiRequest(
        UUID orderId,
        UUID orderItemId,
        Integer quantity
) {
}
