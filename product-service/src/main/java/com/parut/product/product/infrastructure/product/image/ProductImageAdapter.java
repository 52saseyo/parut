package com.parut.product.product.infrastructure.product.image;

import com.parut.product.image.application.dto.LinkedImage;
import com.parut.product.image.application.service.ProductImageService;
import com.parut.product.product.application.product.port.out.ProductImagePort;
import com.parut.product.product.application.product.port.out.dto.ProductImageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductImageAdapter implements ProductImagePort {

    private final ProductImageService productImageService;

    @Override
    public void save(UUID uploadId, UUID productId, UUID imageId) {
        productImageService.registerImage(uploadId, productId, imageId);
    }

    @Override
    public Optional<ProductImageResult> findImage(UUID productId) {
        return productImageService.getImageInfo(productId)
                .map(image -> new ProductImageResult(
                        image.imageId(),
                        image.imageUrl()
                ));
    }

    @Override
    public Map<UUID, ProductImageResult> findImages(Collection<UUID> productIds) {
        return productImageService.getImageInfos(productIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> toResult(entry.getValue())
                ));
    }

    private ProductImageResult toResult(LinkedImage image) {
        return new ProductImageResult(
                image.imageId(),
                image.imageUrl()
        );
    }

    @Override
    public boolean hasImage(UUID productId) {
        return productImageService.hasImage(productId);
    }
}
