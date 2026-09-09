package com.parut.product.product.application.product.query.result;

import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;

import java.util.UUID;

public record PublicProductQueryResult(
        UUID productId,
        String name,
        ProductCategory category,
        Long price,
        AppearanceType appearanceType,
        String origin,
        String mainImageKey
) {
}
