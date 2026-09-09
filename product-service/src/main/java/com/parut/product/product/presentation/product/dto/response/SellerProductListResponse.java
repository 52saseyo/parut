package com.parut.product.product.presentation.product.dto.response;

import com.parut.product.product.application.product.query.result.SellerProductQueryResult;
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
    public static SellerProductListResponse from(SellerProductQueryResult result) {
        return new SellerProductListResponse(
                result.productId(),
                result.name(),
                result.category(),
                result.price(),
                result.status(),
                result.mainImageKey()
        );
    }
}
