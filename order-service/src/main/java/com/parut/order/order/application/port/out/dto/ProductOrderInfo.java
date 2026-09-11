package com.parut.order.order.application.port.out.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductOrderInfo(
        UUID productId,
        UUID sellerId,
        String productName,
        String appearanceType,
        String origin,
        LocalDate harvestDate,
        String saleUnit,
        BigDecimal unitQuantity,
        long originalPrice,
        boolean purchasable
) {
}
