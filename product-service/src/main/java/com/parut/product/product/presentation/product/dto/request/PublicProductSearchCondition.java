package com.parut.product.product.presentation.product.dto.request;

import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;

public record PublicProductSearchCondition(
        String keyword,
        ProductCategory category,
        AppearanceType appearanceType,
        Long minPrice,
        Long maxPrice
) {
}
