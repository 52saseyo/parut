package com.parut.product.product.domain.product;

import com.parut.product.global.common.entity.DeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "p_product_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends DeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    private ProductImageType imageType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /**
     * 상품에 속하는 이미지를 생성한다.
     */
    static ProductImage create(
            Product product,
            String imageKey,
            ProductImageType imageType,
            Integer sortOrder) {
        return new ProductImage(
                product,
                imageKey,
                imageType,
                sortOrder
        );
    }

    /**
     * 상품 이미지의 소속 상품, 저장 키, 유형 및 노출 순서를 설정한다.
     */
    private ProductImage(
            Product product,
            String imageKey,
            ProductImageType imageType,
            Integer sortOrder
    ) {
        this.product = product;
        this.imageKey = imageKey;
        this.imageType = imageType;
        this.sortOrder = sortOrder;
    }

    /**
     * 삭제되지 않은 이미지의 유형을 변경한다.
     */
    void changeImageType(ProductImageType imageType) {
        validateNotDeleted();
        validateImageType(imageType);
        this.imageType = imageType;
    }


    /**
     * 삭제되지 않은 이미지의 노출 순서를 변경한다.
     */
    void changeSortOrder(Integer sortOrder) {
        validateNotDeleted();
        validateSortOrder(sortOrder);
        this.sortOrder = sortOrder;
    }


    /**
     * 상품 이미지를 소프트 삭제한다.
     */
    void delete(String deletedBy) {
        if (isDeleted()) {
            throw new IllegalStateException(
                    "이미 삭제된 상품 이미지입니다."
            );
        }
        softDelete(deletedBy);
    }


    /**
     * 상품 이미지가 삭제되지 않은 상태인지 검증한다.
     */
    private void validateNotDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException(
                    "삭제된 상품 이미지는 변경할 수 없습니다."
            );
        }
    }

    private static void validateImageType(
            ProductImageType imageType
    ) {
        if (imageType == null) {
            throw new IllegalArgumentException(
                    "이미지 유형은 필수입니다."
            );
        }
    }

    private static void validateSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException(
                    "이미지 노출 순서는 0 이상이어야 합니다."
            );
        }
    }

}
