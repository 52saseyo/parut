package com.parut.product.product.application.stock.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductStockHistoryResult(
        String productName,
        List<ProductStockHistoryItem> items,
        String nextCursor
) {

}
