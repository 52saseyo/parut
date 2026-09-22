package com.parut.order.order.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

// TimeDealDetailResponse(product-service) 응답 바디 중 order가 쓰는 필드만 선언, 쓰지 않는 필드는 무시한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TimeDealInfoApiResponse(
        UUID timeDealId,
        UUID productId, // productId는 직접 등록 타임딜이면 null
        UUID sellerId,
        String productName,
        Long originalPrice,
        Long dealPrice,
        String productGrade,
        String origin,
        LocalDate harvestedDate
) {
}
