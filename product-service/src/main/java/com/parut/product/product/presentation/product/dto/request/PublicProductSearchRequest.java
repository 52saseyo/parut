package com.parut.product.product.presentation.product.dto.request;

import com.parut.product.product.application.product.query.condition.PublicProductSearchCondition;
import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;

public record PublicProductSearchRequest(
        String keyword,
        ProductCategory category,
        AppearanceType appearanceType,
        ProductStatus status,
        Long minPrice,
        Long maxPrice
) {
    public PublicProductSearchCondition toCondition() {
        return new PublicProductSearchCondition(
                keyword,
                category,
                appearanceType,
                status,
                minPrice,
                maxPrice
        );
    }
}
