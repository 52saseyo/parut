package com.parut.product.image.domain.productImage;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_product_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends DeletableEntity {
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "image_id", nullable = false, updatable = false)
    private UUID imageId;

    @Column(name = "image_url", nullable = false, updatable = false, length = 1000)
    private String imageUrl;

    private ProductImage(
            UUID productId,
            UUID imageId,
            String imageUrl
    ) {
        validateProductId(productId);
        validateImageId(imageId);
        validateImageUrl(imageUrl);

        this.productId = productId;
        this.imageId = imageId;
        this.imageUrl = imageUrl;
    }

    public static ProductImage create(
            UUID productId,
            UUID imageId,
            String imageUrl
    ) {
        return new ProductImage(
                productId,
                imageId,
                imageUrl
        );
    }

    public void delete(String deletedBy) {
        if (!isDeleted()) {
            softDelete(deletedBy);
        }
    }

    private static void validateProductId(UUID productId) {
        if (productId == null) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_PRODUCT_ID_REQUIRED);
        }
    }

    private static void validateImageId(UUID imageId) {
        if (imageId == null) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_ID_REQUIRED);
        }
    }

    private static void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_URL);
        }
    }
}
