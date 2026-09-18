package com.parut.product.product.application.product.port.out;

import com.parut.product.product.application.product.port.out.dto.ProductImageResult;

import java.util.Optional;
import java.util.UUID;

public interface ProductImagePort {

    void save(
            UUID uploaderId,
            UUID productId,
            UUID imageId
    );

    Optional<ProductImageResult> findImage(UUID productId);

    boolean hasImage(UUID productId);
}
