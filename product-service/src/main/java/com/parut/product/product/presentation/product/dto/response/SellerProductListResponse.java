package com.parut.product.product.presentation.product.dto.response;

import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;

import java.util.UUID;

public record SellerProductListResponse(
        UUID productId,
        String name,
        ProductCategory category,
        Long price,
        ProductStatus status,
        String mainImageKey
) {
}
