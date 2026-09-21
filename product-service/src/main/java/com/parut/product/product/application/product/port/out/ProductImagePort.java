package com.parut.product.product.application.product.port.out;

import com.parut.product.product.application.product.port.out.dto.ProductImageResult;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProductImagePort {

    void save(
            UUID uploaderId,
            UUID productId,
            UUID imageId
    );

    Optional<ProductImageResult> findImage(UUID productId);

    Map<UUID, ProductImageResult> findImages(Collection<UUID> productIds);

    boolean hasImage(UUID productId);
}
