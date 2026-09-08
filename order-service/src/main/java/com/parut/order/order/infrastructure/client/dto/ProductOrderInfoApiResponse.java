package com.parut.order.order.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// ProductOrderInfoResponse(product-service) 응답 바디와 동일한 필드 구조
public record ProductOrderInfoApiResponse(
        UUID productId,
        UUID stockId,
        UUID sellerId,
        String productName,
        String appearanceType,
        String origin,
        LocalDate harvestDate,
        String saleUnit,
        BigDecimal unitQuantity,
        Long originalPrice,
        String saleStatus,
        boolean purchasable
) {
}
