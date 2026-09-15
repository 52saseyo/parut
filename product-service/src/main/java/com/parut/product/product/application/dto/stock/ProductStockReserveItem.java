package com.parut.product.product.application.dto.stock;

import java.util.UUID;

public record ProductStockReserveItem (
            UUID productId,
            UUID orderItemId,
            int quantity
) {

}
