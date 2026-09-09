package com.parut.product.product.domain.product;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
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
            Integer sortOrder
    ) {
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
        validateProduct(product);
        validateImageKey(imageKey);
        validateImageType(imageType);
        validateSortOrder(sortOrder);
        this.product = product;
        this.imageKey = imageKey;
        this.imageType = imageType;
        this.sortOrder = sortOrder;
    }

    /**
     * 이미지를 소프트 삭제한다.
     *
     * <p>이미 삭제된 이미지에 대해서는 아무 작업도 수행하지 않는다.</p>
     */
    void delete(String deletedBy){
        if(isDeleted()){
            return;
        }
        softDelete(deletedBy);
    }

    /**
     * 상세 이미지의 노출 순서만 변경한다.
     *
     * <p>이미지 유형은 변경하지 않으며 대표 이미지에는 사용할 수 없다.</p>
     */
    void changeDetailSortOrder(Integer sortOrder) {
        if (imageType != ProductImageType.DETAIL) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_INVALID_TYPE);
        }

        if (sortOrder == null || sortOrder < 1) {
            throw new BusinessException(ErrorCode.PRODUCT_DETAIL_IMAGE_SORT_ORDER_INVALID);
        }

        this.sortOrder = sortOrder;
    }



    /**
     * 이미지가 속할 상품이 입력되었는지 검증한다.
     */
    private static void validateProduct(Product product) {
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_PRODUCT_REQUIRED);
        }
    }

    /**
     * 이미지 키가 필수 조건과 최대 길이를 만족하는지 검증한다.
     */
    private static void validateImageKey(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_INVALID_KEY);
        }

        if (imageKey.length() > 500) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_INVALID_KEY);
        }
    }


    /**
     * 이미지 유형이 입력되었는지 검증한다.
     */
    private static void validateImageType(ProductImageType imageType) {
        if (imageType == null) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_INVALID_TYPE);
        }
    }

    /**
     * 이미지 노출 순서가 입력되었고 0 이상인지 검증한다.
     */
    private static void validateSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 0){
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_INVALID_SORT_ORDER);
        }
    }



}
