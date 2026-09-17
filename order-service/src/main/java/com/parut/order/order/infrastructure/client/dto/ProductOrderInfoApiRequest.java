package com.parut.order.order.infrastructure.client.dto;

import java.util.List;
import java.util.UUID;

// ProductOrderInfoRequest(product-service)와 동일한 필드 구조
public record ProductOrderInfoApiRequest(
        List<UUID> productIds
) {
}
