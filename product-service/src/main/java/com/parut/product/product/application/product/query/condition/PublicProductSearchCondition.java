package com.parut.product.product.application.product.query.condition;

import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;

public record PublicProductSearchCondition(
        String keyword,
        ProductCategory category,
        AppearanceType appearanceType,
        ProductStatus status,
        Long minPrice,
        Long maxPrice
) {
}
