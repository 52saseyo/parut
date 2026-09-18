package com.parut.product.image.presentation.dto.response;

import com.parut.product.image.application.dto.ImageUploadUrlResult;

public record ImageUploadUrlResponse(
        String imageKey,
        String uploadUrl
) {
    public static ImageUploadUrlResponse from(
            ImageUploadUrlResult result
    ) {
        return new ImageUploadUrlResponse(
                result.imageKey(),
                result.uploadUrl()
        );
    }
}
