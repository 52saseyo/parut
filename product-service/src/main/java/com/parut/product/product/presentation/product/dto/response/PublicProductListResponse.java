package com.parut.product.product.presentation.product.dto.response;

import com.parut.product.product.application.product.query.result.PublicProductQueryResult;
import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;

import java.util.UUID;

public record PublicProductListResponse(
        UUID productId,
        String name,
        ProductCategory category,
        Long price,
        AppearanceType appearanceType,
        String origin,
        String mainImageKey
) {
    public static PublicProductListResponse from(PublicProductQueryResult result) {
        return new PublicProductListResponse(
                result.productId(),
                result.name(),
                result.category(),
                result.price(),
                result.appearanceType(),
                result.origin(),
                result.mainImageKey()
        );
    }
}
