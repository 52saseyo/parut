package com.parut.product.product.presentation.product.dto.response;

import com.parut.product.product.domain.product.ProductImage;
import com.parut.product.product.domain.product.ProductImageType;

import java.util.UUID;

public record ProductImageResponse(
        UUID imageId,
        String imageKey,
        ProductImageType imageType,
        Integer sortOrder
) {
    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageKey(),
                image.getImageType(),
                image.getSortOrder()
        );
    }
}
