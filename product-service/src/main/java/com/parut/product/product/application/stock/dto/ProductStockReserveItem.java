package com.parut.product.product.application.stock.dto;

import java.util.UUID;

public record ProductStockReserveItem (
            UUID productId,
            UUID orderItemId,
            int quantity
) {

}
