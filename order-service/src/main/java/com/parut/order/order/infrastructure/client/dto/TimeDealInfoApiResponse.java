package com.parut.order.order.infrastructure.client.dto;

import java.util.UUID;

// GET /internal/time-deals/{timeDealId}(product-service) 응답 바디와 동일한 필드 구조
public record TimeDealInfoApiResponse(
        UUID timeDealId,
        UUID productId,
        UUID sellerId,
        String productName,
        Long dealPrice
) {
}
