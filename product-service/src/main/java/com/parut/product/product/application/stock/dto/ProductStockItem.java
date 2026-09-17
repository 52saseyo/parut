package com.parut.product.product.application.stock.dto;

import java.util.UUID;

public record ProductStockItem(
        UUID productId,
        UUID orderItemId
){
}
