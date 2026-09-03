package com.parut.product.product.domain.product;

import com.parut.product.global.common.entity.DeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

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

        this.status = ProductStatus.ON_SALE;
    }

    /**
     * 상품과 상품에 속한 활성 이미지를 함께 소프트 삭제한다.
     */
    public void delete(String deletedBy) {
        if(status == ProductStatus.DELETED || isDeleted()) {
            throw new IllegalStateException("이미 삭제된 상품입니다.");
        }
        productImages.stream()
                .filter(image -> !image.isDeleted())
                .forEach(image -> image.delete(deletedBy));

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
        if(imageType == ProductImageType.MAIN){
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
     * 상품에 속한 활성 이미지를 새로운 대표 이미지로 지정한다.
     * 기존 대표 이미지가 있으면 상세 이미지로 변경한다.
     */
    public void changeMainImage(ProductImage newMainImage) {
        validateModifiable();
        validateProductImage(newMainImage);

        if (newMainImage.getImageType() == ProductImageType.MAIN) {
            return;
        }
        productImages.stream()
                .filter(image -> !image.isDeleted())
                .filter(image ->
                        image.getImageType() == ProductImageType.MAIN
                )
                .findFirst()
                .ifPresent(currentMain ->
                        currentMain.changeImageType(
                                ProductImageType.DETAIL
                        )
                );

        newMainImage.changeImageType(ProductImageType.MAIN);
    }

    /**
     * 상품에 속한 활성 이미지의 노출 순서를 변경한다.
     */
    public void changeProductImageSortOrder(
            ProductImage image,
            int sortOrder
    ) {
        validateModifiable();
        validateProductImage(image);

        image.changeSortOrder(sortOrder);
    }


    /**
     * 상품에 속한 이미지를 소프트 삭제한다.
     * 판매 중인 상품의 대표 이미지는 삭제할 수 없다.
     */
    public void deleteProductImage(
            ProductImage image,
            String deletedBy
    ) {
        validateModifiable();
        validateProductImage(image);

        if (status == ProductStatus.ON_SALE && image.getImageType() == ProductImageType.MAIN) {
            throw new IllegalStateException("판매 중인 상품의 대표 이미지는 삭제할 수 없습니다.");
        }
        image.delete(deletedBy);
    }



    /**
     * 삭제되지 않은 대표 이미지가 이미 존재하는지 검증한다.
     */
    private void validateMainImageNotExists(){
        boolean mainImageExists = productImages.stream()
                .filter(image -> !image.isDeleted())
                .anyMatch(image ->
                        image.getImageType() == ProductImageType.MAIN
                );

        if (mainImageExists) {
            throw new IllegalStateException("대표 이미지는 하나만 등록할 수 있습니다.");
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

    /**
     * 이미지가 해당 상품에 속한 삭제되지 않은 이미지인지 검증한다.
     */
    private void validateProductImage(ProductImage image) {
        if (image == null || !productImages.contains(image)) {
            throw new IllegalArgumentException("해당 상품에 속하지 않은 이미지입니다.");
        }

        if (image.isDeleted()) {
            throw new IllegalStateException("이미 삭제된 상품 이미지입니다.");
        }
    }



}
