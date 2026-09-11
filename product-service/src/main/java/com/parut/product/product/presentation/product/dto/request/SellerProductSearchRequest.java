package com.parut.product.product.presentation.product.dto.request;

import com.parut.product.product.application.product.query.condition.SellerProductSearchCondition;
import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;

public record SellerProductSearchRequest(
        String keyword,
        ProductCategory category,
        ProductStatus status,
        AppearanceType appearanceType
) {
    public SellerProductSearchCondition toCondition() {
        return new SellerProductSearchCondition(
                keyword,
                category,
                status,
                appearanceType
        );
    }
}
