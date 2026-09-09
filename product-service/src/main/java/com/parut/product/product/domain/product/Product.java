package com.parut.product.product.domain.product;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends DeletableEntity {
    private static final int MAX_DETAIL_IMAGE_COUNT = 5;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

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

    @Getter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "product",
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE
            }
    )
    @OrderBy("sortOrder ASC")
    private List<ProductImage> productImages = new ArrayList<>();



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
        validateMainImageExists();
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
        validateMainImageExists();
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
        validateMainImageExists();
        this.status = ProductStatus.ON_SALE;
    }

    /**
     * 상품을 삭제 상태로 전환하고 소프트 삭제한다.
     */
    public void delete(String deletedBy) {
        if(status == ProductStatus.DELETED || isDeleted()) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_DELETED);
        }

        productImages.stream()
                .filter(image -> !image.isDeleted())
                .forEach(image -> image.delete(deletedBy));

        this.status = ProductStatus.DELETED;
        softDelete(deletedBy);
    }

    /**
     * 대표 이미지가 없는 상품에 대표 이미지를 추가한다.
     *
     * 대표 이미지는 항상 노출 순서 0을 사용하며,
     * 활성 대표 이미지가 이미 있으면 추가할 수 없다.
     */
    public ProductImage addMainImage(String imageKey) {
        validateImageModifiable();
        validateImageKeyNotExists(imageKey);
        validateMainImageNotExists();

        ProductImage image = ProductImage.create(
                this,
                imageKey,
                ProductImageType.MAIN,
                0
        );
        productImages.add(image);
        return image;
    }

    /**
     * 상세 이미지를 마지막 노출 순서로 추가한다.
     *
     * 클라이언트로부터 순서를 받지 않고 서버가 활성 상세 이미지의
     * 마지막 순서 다음 값을 계산하여 적용한다.
     */
    public ProductImage addDetailImage(String imageKey) {
        validateImageModifiable();
        validateDetailImageCount();
        validateImageKeyNotExists(imageKey);

        int sortOrder = getNextDetailImageSortOrder();

        ProductImage image = ProductImage.create(
                this,
                imageKey,
                ProductImageType.DETAIL,
                sortOrder
        );
        productImages.add(image);
        return image;
    }

    /**
     * 상품 이미지를 소프트 삭제한다.
     *
     * DRAFT 또는 SUSPENDED 상태에서는 대표 이미지와
     * 상세 이미지를 모두 삭제할 수 있다. 상세 이미지를 삭제하면
     * 뒤에 있는 상세 이미지의 순서를 하나씩 당긴다.
     */
    public String removeProductImage(UUID imageId, String deletedBy) {
        validateImageModifiable();
        ProductImage image = findActiveImage(imageId);
        boolean detailImage = image.getImageType() == ProductImageType.DETAIL;
        int removedSortOrder = image.getSortOrder();
        String deletedImageKey = image.getImageKey();

        image.delete(deletedBy);

        if (detailImage) {
            compactDetailImageOrders(removedSortOrder);
        }

        return deletedImageKey;
    }

    /**
     * 삭제되지 않은 상품 이미지를 노출 순서 오름차순으로 반환한다.
     */
    public List<ProductImage> getActiveProductImages() {
        return productImages.stream()
                .filter(image -> !image.isDeleted())
                .sorted(Comparator.comparing(ProductImage::getSortOrder))
                .toList();
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

    /**
     * 삭제되지 않은 활성 대표 이미지가 존재하는지 확인한다.
     */
    private boolean hasActiveMainImage() {
        return productImages.stream()
                .filter(image -> !image.isDeleted())
                .anyMatch(image ->
                        image.getImageType() == ProductImageType.MAIN
                );
    }

    /**
     * 판매 시작에 필요한 활성 대표 이미지가 존재하는지 검증한다.
     */
    private void validateMainImageExists() {
        if (!hasActiveMainImage()) {
            throw new BusinessException(ErrorCode.PRODUCT_MAIN_IMAGE_REQUIRED);
        }
    }

    /**
     * 활성 대표 이미지가 중복 등록되지 않도록 검증한다.
     */
    private void validateMainImageNotExists() {
        if (hasActiveMainImage()) {
            throw new BusinessException(ErrorCode.PRODUCT_MAIN_IMAGE_ALREADY_EXISTS);
        }
    }

    /**
     * 현재 상품에서 삭제되지 않은 이미지가 같은 키를 사용하고 있지 않은지 검증한다.
     */
    private void validateImageKeyNotExists(String imageKey) {
        boolean exists = productImages.stream()
                .filter(image -> !image.isDeleted())
                .anyMatch(image ->
                        image.getImageKey().equals(imageKey)
                );

        if (exists) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_ALREADY_EXISTS);
        }
    }

    /**
     * 현재 상품의 활성 상세 이미지 개수가 최대 등록 개수(5장)를 넘지 않는지 검증한다.
     */
    private void validateDetailImageCount(){
        long activeDetailImageCount = productImages.stream()
                .filter(image -> !image.isDeleted())
                .filter(image -> image.getImageType() == ProductImageType.DETAIL)
                .count();
        if(activeDetailImageCount >= MAX_DETAIL_IMAGE_COUNT) {
               throw new BusinessException(ErrorCode.PRODUCT_DETAIL_IMAGE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 상품이 이미지 등록·교체·삭제가 가능한 상태인지 검증한다.
     * 삭제된 상품과 판매 중 또는 품절 상태의 상품은 변경할 수 없다.
     */
    private void validateImageModifiable(){
        if(isDeleted() || (status != ProductStatus.DRAFT
                && status != ProductStatus.SUSPENDED)){
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_NOT_MODIFIABLE);
        }
    }

    /**
     * 새 상세 이미지가 사용할 다음 노출 순서를 계산한다.
     */
    private int getNextDetailImageSortOrder() {
        return productImages.stream()
                .filter(image -> !image.isDeleted())
                .filter(image -> image.getImageType() == ProductImageType.DETAIL)
                .mapToInt(ProductImage::getSortOrder)
                .max()
                .orElse(0) + 1;
    }

    /**
     * 삭제된 상세 이미지보다 뒤에 있는 상세 이미지의 순서를 하나씩 당긴다.
     */
    private void compactDetailImageOrders(int removedSortOrder) {
        productImages.stream()
                .filter(image -> !image.isDeleted())
                .filter(image -> image.getImageType() == ProductImageType.DETAIL)
                .filter(image -> image.getSortOrder() > removedSortOrder)
                .forEach(image -> image.changeDetailSortOrder(image.getSortOrder() - 1));
    }

    /**
     * 상품에 속한 활성 이미지를 ID로 조회한다.
     * 조회 대상이 없으면 이미지 없음 예외를 발생시킨다.
     */
    private ProductImage findActiveImage(UUID imageId){
        if (imageId == null) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND);
        }
        return productImages.stream()
                .filter(image -> !image.isDeleted())
                .filter(image -> imageId.equals(image.getId()))
                .findFirst()
                .orElseThrow(()->new BusinessException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));
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
