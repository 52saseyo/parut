package com.parut.product.product.presentation.product.dto.response;

import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.domain.stock.entity.ProductStock;

import java.util.UUID;

public record ProductOrderInfoResponse(
        UUID productId,
        UUID stockId,
        UUID sellerId,
        String productName,
        Long unitPrice,
        ProductStatus saleStatus,
        boolean purchasable
) {
    public static ProductOrderInfoResponse from(Product product, ProductStock productStock, boolean purchasable) {

        return new ProductOrderInfoResponse(
                product.getId(),
                productStock.getId(),
                product.getSellerId(),
                product.getName(),
                product.getPrice(),
                product.getStatus(),
                purchasable
        );
    }
}
