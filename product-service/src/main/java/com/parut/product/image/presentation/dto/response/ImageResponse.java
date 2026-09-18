package com.parut.product.image.presentation.dto.response;

import com.parut.product.image.domain.image.Image;

import java.util.UUID;

public record ImageResponse(
        UUID imageId,
        String imageUrl
) {
    public static ImageResponse from(Image image) {
        return new ImageResponse(
                image.getId(),
                image.getImageUrl()
        );
    }
}
