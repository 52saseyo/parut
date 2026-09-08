package com.parut.product.product.presentation.product.dto.request;

import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;

public record SellerProductSearchCondition(
        String keyword,
        ProductCategory category,
        ProductStatus status,
        Long minPrice,
        Long maxPrice
){
}
