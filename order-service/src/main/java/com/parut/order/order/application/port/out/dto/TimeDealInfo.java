package com.parut.order.order.application.port.out.dto;

import java.util.UUID;

public record TimeDealInfo(
        UUID timeDealId,
        UUID productId,
        UUID sellerId,
        String productName,
        long dealPrice
) {
}
