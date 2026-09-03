package com.parut.product.product.domain.product;

import com.parut.product.global.common.entity.DeletableEntity;
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
}
