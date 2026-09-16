package com.parut.product.product.application.stock.dto;

import java.util.List;

public record ProductStockHistoryResult(
        String productName,
        List<ProductStockHistoryItem> items
) {

}
