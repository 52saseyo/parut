package com.parut.product.product.domain.product;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends DeletableEntity {

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "image_id")
    private UUID imageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ProductCategory category;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "price", nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(name = "appearance_type", nullable = false, length = 20)
    private AppearanceType appearanceType;

    @Column(name = "origin", nullable = false, length = 100)
    private String origin;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_unit", nullable = false, length = 20)
    private SaleUnit saleUnit;

    @Column(name = "unit_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;





    /**
     * 상품을 판매 준비 상태({@link ProductStatus#DRAFT})로 생성한다.
     */
    public static Product create(
            UUID sellerId,
            ProductCategory category,
            String name,
            String description,
            Long price,
            AppearanceType appearanceType,
            String origin,
            LocalDate harvestDate,
            SaleUnit saleUnit,
            BigDecimal unitQuantity
    ) {
        return new Product(
                sellerId,
                category,
                name,
                description,
                price,
                appearanceType,
                origin,
                harvestDate,
                saleUnit,
                unitQuantity
        );
    }


    /**
     * 상품 생성에 필요한 값을 설정하고 최초 상태를 판매 준비 상태로 초기화한다.
     */
    private Product(
            UUID sellerId,
            ProductCategory category,
            String name,
            String description,
            Long price,
            AppearanceType appearanceType,
            String origin,
            LocalDate harvestDate,
            SaleUnit saleUnit,
            BigDecimal unitQuantity
    ) {
        validateSellerId(sellerId);
        validateCategory(category);
        validateName(name);
        validatePrice(price);
        validateAppearanceType(appearanceType);
        validateOrigin(origin);
        validateHarvestDate(harvestDate);
        validateSaleUnit(saleUnit);
        validateUnitQuantity(unitQuantity);

        this.sellerId = sellerId;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.appearanceType = appearanceType;
        this.origin = origin;
        this.harvestDate = harvestDate;
        this.saleUnit = saleUnit;
        this.unitQuantity = unitQuantity;

        // 최초 상품 상태
        this.status = ProductStatus.DRAFT;
    }

    /**
     * 상품의 기본 정보를 수정한다.
     */
    public void update(
            ProductCategory category,
            String name,
            String description,
            Long price,
            AppearanceType appearanceType,
            String origin,
            LocalDate harvestDate,
            SaleUnit saleUnit,
            BigDecimal unitQuantity
    ) {
        validateModifiable();

        if (category != null) {
            this.category = category;
        }

        if (name != null) {
            validateName(name);
            this.name = name;
        }

        if (description != null) {
            this.description = description;
        }

        if (price != null) {
            validatePrice(price);
            this.price = price;
        }

        if (appearanceType != null) {
            this.appearanceType = appearanceType;
        }

        if (origin != null) {
            validateOrigin(origin);
            this.origin = origin;
        }

        if (harvestDate != null) {
            this.harvestDate = harvestDate;
        }

        if (saleUnit != null) {
            this.saleUnit = saleUnit;
        }

        if (unitQuantity != null) {
            validateUnitQuantity(unitQuantity);
            this.unitQuantity = unitQuantity;
        }
    }

    /**
     * 판매 준비 상태의 상품을 판매 중 상태로 전환한다.
     */
    public void startSale(){
        validateNotDeleted();
        if (status != ProductStatus.DRAFT) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED);
        }
        validateImageExists();
        this.status = ProductStatus.ON_SALE;
    }


    /**
     * 판매 중인 상품을 품절 상태로 전환한다.
     */
    public void soldOut(){
        validateNotDeleted();
        if (status != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED);
        }
        this.status = ProductStatus.SOLD_OUT;
    }

    /**
     * 재입고된 품절 상품을 판매 중 상태로 전환한다.
     */
    public void resumeSaleAfterRestock() {
        validateNotDeleted();
        if (status != ProductStatus.SOLD_OUT) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED);
        }
        validateImageExists();
        this.status = ProductStatus.ON_SALE;
    }

    /**
     * 판매 중인 상품을 판매 중지 상태로 전환한다.
     */
    public void suspend() {
        validateNotDeleted();
        if (status != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED);
        }

        this.status = ProductStatus.SUSPENDED;
    }

    /**
     * 판매 중지된 상품을 재개한다.
     */
    public void resumeSale() {
        validateNotDeleted();

        if (status != ProductStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED);
        }
        validateImageExists();
        this.status = ProductStatus.ON_SALE;
    }

    /**
     * 상품을 삭제 상태로 전환하고 소프트 삭제한다.
     */
    public void delete(String deletedBy) {
        if(status == ProductStatus.DELETED || isDeleted()) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_DELETED);
        }

        this.status = ProductStatus.DELETED;
        softDelete(deletedBy);
    }




    /**
     * 상품이 삭제되었는 지 검증한다.
     */
    private void validateNotDeleted() {
        if (status == ProductStatus.DELETED || isDeleted()) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_NOT_MODIFIABLE
            );
        }
    }

    /**
     * 상품이 변경 가능한 상태인지 검증한다.
     */
    private void validateModifiable() {
        validateNotDeleted();

        if (status != ProductStatus.DRAFT
                && status != ProductStatus.SUSPENDED) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_NOT_MODIFIABLE
            );
        }
    }



    public void addImage(UUID imageId) {
        validateModifiable();
        validateImageId(imageId);

        if (this.imageId != null) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_IMAGE_ALREADY_EXISTS
            );
        }
        this.imageId = imageId;
    }


    public UUID changeImage(UUID newImageId) {
        validateModifiable();
        validateImageId(newImageId);

        if (this.imageId == null) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_IMAGE_NOT_FOUND
            );
        }

        UUID previousImageId = this.imageId;
        this.imageId = newImageId;

        return previousImageId;
    }


    public UUID removeImage() {
        validateModifiable();
        if (this.imageId == null) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_IMAGE_NOT_FOUND
            );
        }
        UUID removedImageId = this.imageId;
        this.imageId = null;

        return removedImageId;
    }

    private void validateImageId(UUID imageId) {
        if (imageId == null) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_IMAGE_ID_REQUIRED
            );
        }
    }

    private void validateImageExists(){
        if(imageId == null){
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_REQUIRED);
        }
    }


    private static void validateSellerId(UUID sellerId) {
        if (sellerId == null) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_SELLER_ID);
        }
    }

    private static void validateCategory(ProductCategory category) {
        if (category == null) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_CATEGORY);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_NAME);
        }

        if (name.length() > 150) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_NAME);
        }
    }

    private static void validatePrice(Long price) {
        if (price == null) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_PRICE);
        }

        if (price < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_PRICE);
        }
    }

    private static void validateAppearanceType(AppearanceType appearanceType) {
        if (appearanceType == null) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_APPEARANCE_TYPE);
        }
    }

    private static void validateOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_ORIGIN);
        }

        if (origin.length() > 100) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_ORIGIN);
        }
    }

    private static void validateHarvestDate(LocalDate harvestDate) {
        if (harvestDate == null) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_HARVEST_DATE);
        }
    }

    private static void validateSaleUnit(SaleUnit saleUnit) {
        if (saleUnit == null) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_SALE_UNIT);
        }
    }

    private static void validateUnitQuantity(BigDecimal unitQuantity) {
        if (unitQuantity == null) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_UNIT_QUANTITY);
        }

        if (unitQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_UNIT_QUANTITY);
        }
    }

}
