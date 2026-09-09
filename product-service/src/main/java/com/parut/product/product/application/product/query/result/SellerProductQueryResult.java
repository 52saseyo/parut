package com.parut.product.product.application.product.query.result;

import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;

import java.util.UUID;

public record SellerProductQueryResult(
        UUID productId,
        String name,
        ProductCategory category,
        Long price,
        ProductStatus status,
        String mainImageKey
) {
}
