package com.parut.order.order.application.port.out.dto;

import java.util.UUID;

public record ProductStockItem(
        UUID productId,
        UUID orderItemId
) {
}
