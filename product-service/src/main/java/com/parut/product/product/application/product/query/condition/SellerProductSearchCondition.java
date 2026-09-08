package com.parut.product.product.application.product.query.condition;

import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;

public record SellerProductSearchCondition(
        String keyword,
        ProductCategory category,
        ProductStatus status,
        AppearanceType appearanceType
) {
}
