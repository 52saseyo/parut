package com.parut.product.product.domain.product;

import com.parut.product.global.common.entity.DeletableEntity;
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
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends DeletableEntity {

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
     * 판매 준비 상태의 상품을 판매 중 상태로 전환한다.
     */
    public void startSale(){
        validateModifiable();
        if (status != ProductStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태의 상품만 판매를 시작할 수 있습니다.");
        }
        validateMainImageExists();
        this.status = ProductStatus.ON_SALE;
    }


    /**
     * 판매 중인 상품을 품절 상태로 전환한다.
     */
    public void soldOut(){
        validateModifiable();
        if (status != ProductStatus.ON_SALE) {
            throw new IllegalStateException("판매 중인 상품만 품절 처리할 수 있습니다.");
        }
        this.status = ProductStatus.SOLD_OUT;
    }

    /**
     * 판매 중이거나 품절된 상품을 판매 중지 상태로 전환한다.
     */
    public void suspend() {
        validateModifiable();
        if (status != ProductStatus.ON_SALE && status != ProductStatus.SOLD_OUT) {
            throw new IllegalStateException("판매 중이거나 품절된 상품만 판매 중지할 수 있습니다.");
        }

        this.status = ProductStatus.SUSPENDED;
    }

    /**
     * 판매 중지된 상품을 재개한다.
     */
    public void resumeSale() {
        validateModifiable();

        if (status != ProductStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "판매 중지된 상품만 판매를 재개할 수 있습니다."
            );
        }
        validateMainImageExists();
        this.status = ProductStatus.ON_SALE;
    }

    /**
     * 상품을 삭제 상태로 전환하고 소프트 삭제한다.
     */
    public void delete(String deletedBy) {
        if(status == ProductStatus.DELETED || isDeleted()) {
            throw new IllegalStateException("이미 삭제된 상품입니다.");
        }

        this.status = ProductStatus.DELETED;
        softDelete(deletedBy);
    }

    /**
     * 상품에 이미지를 추가한다. 활성 대표 이미지는 하나만 등록하게 제한한다.
     */
    public ProductImage addProductImage(
            String imageKey,
            ProductImageType imageType,
            int sortOrder
    ) {
        validateModifiable();
        if (imageType == ProductImageType.MAIN) {
            validateMainImageNotExists();
        }

        ProductImage image = ProductImage.create(
                this,
                imageKey,
                imageType,
                sortOrder
        );
        productImages.add(image);
        return image;
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
            throw new IllegalStateException(
                    "대표 이미지가 등록된 상품만 판매를 시작할 수 있습니다."
            );
        }
    }

    /**
     * 활성 대표 이미지가 중복 등록되지 않도록 검증한다.
     */
    private void validateMainImageNotExists() {
        if (hasActiveMainImage()) {
            throw new IllegalStateException(
                    "대표 이미지는 하나만 등록할 수 있습니다."
            );
        }
    }

    /**
     * 상품이 변경 가능한 상태인지 검증한다.
     */
    private void validateModifiable() {
        if (status == ProductStatus.DELETED || isDeleted()) {
            throw new IllegalStateException("삭제된 상품은 변경할 수 없습니다.");
        }
    }


    private static void validateSellerId(UUID sellerId) {
        if (sellerId == null) {
            throw new IllegalArgumentException("판매자 ID는 필수입니다.");
        }
    }

    private static void validateCategory(ProductCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("상품 카테고리는 필수입니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }

        if (name.length() > 150) {
            throw new IllegalArgumentException("상품명은 150자를 초과할 수 없습니다.");
        }
    }

    private static void validatePrice(Long price) {
        if (price == null) {
            throw new IllegalArgumentException("상품 가격은 필수입니다.");
        }

        if (price < 0) {
            throw new IllegalArgumentException("상품 가격은 0 이상이어야 합니다.");
        }
    }

    private static void validateAppearanceType(AppearanceType appearanceType) {
        if (appearanceType == null) {
            throw new IllegalArgumentException("상품 외관 유형은 필수입니다.");
        }
    }

    private static void validateOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("원산지는 필수입니다.");
        }

        if (origin.length() > 100) {
            throw new IllegalArgumentException("원산지는 100자를 초과할 수 없습니다.");
        }
    }

    private static void validateHarvestDate(LocalDate harvestDate) {
        if (harvestDate == null) {
            throw new IllegalArgumentException("수확일은 필수입니다.");
        }
    }

    private static void validateSaleUnit(SaleUnit saleUnit) {
        if (saleUnit == null) {
            throw new IllegalArgumentException("판매 단위는 필수입니다.");
        }
    }

    private static void validateUnitQuantity(BigDecimal unitQuantity) {
        if (unitQuantity == null) {
            throw new IllegalArgumentException("판매 단위 수량은 필수입니다.");
        }

        if (unitQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("판매 단위 수량은 0보다 커야 합니다.");
        }
    }

}
