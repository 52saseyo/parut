package com.parut.product.product.infrastructure.product.image;

import com.parut.product.image.application.service.ProductImageService;
import com.parut.product.product.application.product.port.out.ProductImagePort;
import com.parut.product.product.application.product.port.out.dto.ProductImageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

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
    public boolean hasImage(UUID productId) {
        return productImageService.hasImage(productId);
    }
}
