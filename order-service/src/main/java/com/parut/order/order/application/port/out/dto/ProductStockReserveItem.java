package com.parut.order.order.application.port.out.dto;

import java.util.UUID;

public record ProductStockReserveItem(
        UUID productId,
        UUID orderItemId,
        int quantity
) {
}
