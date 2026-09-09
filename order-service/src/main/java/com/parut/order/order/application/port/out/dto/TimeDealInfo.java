package com.parut.order.order.application.port.out.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TimeDealInfo(
        UUID timeDealId,
        UUID productId,
        UUID sellerId,
        String productName,
        long originalPrice,
        long dealPrice,
        String productGrade,
        String origin,
        LocalDate harvestedDate
) {
}
