package com.parut.order.order.infrastructure.client.dto;

import java.time.LocalDate;
import java.util.UUID;

// TimeDealDetailResponse(product-service) 응답 바디와 동일한 필드 구조
public record TimeDealInfoApiResponse(
        UUID timeDealId,
        UUID productId, // productId는 직접 등록 타임딜이면 null
        UUID sellerId,
        UUID imageId,
        String productName,
        Long originalPrice,
        Long dealPrice,
        String productGrade,
        String origin,
        LocalDate harvestedDate
) {
}
