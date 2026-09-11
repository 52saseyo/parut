package com.parut.product.product.presentation.stock.dto.response;

import com.parut.product.product.domain.stock.entity.ProductStock;

import java.util.UUID;

public record ProductStockResponse(
        UUID stockId,
        UUID productId,
        int totalQuantity,
        int availableQuantity,
        String status
) {
    public static ProductStockResponse from(ProductStock stock) {
        return new ProductStockResponse(
                stock.getId(),
                stock.getProductId(),
                stock.getTotalQuantity(),
                stock.getAvailableQuantity(),
                stock.getStatus().name()
        );
    }
}