package com.parut.product.product.application.product.port.out.dto;

import java.util.UUID;

public record ProductImageResult(
        UUID imageId,
        String imageUrl
) {
}
