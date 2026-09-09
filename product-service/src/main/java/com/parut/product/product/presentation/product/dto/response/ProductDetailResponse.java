package com.parut.product.product.presentation.product.dto.response;

import com.parut.product.product.domain.product.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductDetailResponse(
        UUID productId,
        ProductCategory category,
        String name,
        String description,
        Long price,
        AppearanceType appearanceType,
        String origin,
        LocalDate harvestDate,
        SaleUnit saleUnit,
        BigDecimal unitQuantity,
        ProductStatus status

) {
    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getCategory(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getAppearanceType(),
                product.getOrigin(),
                product.getHarvestDate(),
                product.getSaleUnit(),
                product.getUnitQuantity(),
                product.getStatus()
        );
    }
}
