package com.parut.product.image.application.dto;

import java.util.UUID;

public record LinkedImage(
        UUID imageId,
        String imageUrl
) {
}
