package com.parut.product.product.application.dto.stock;

import java.util.UUID;

public record ProductStockItem(
        UUID productId,
        UUID orderItemId
){
}
