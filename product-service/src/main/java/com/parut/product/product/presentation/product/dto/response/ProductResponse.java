package com.parut.product.product.presentation.product.dto.response;

import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductStatus;

import java.util.UUID;

public record ProductResponse(
        UUID productId,
        ProductStatus status
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getStatus()
        );
    }
}
