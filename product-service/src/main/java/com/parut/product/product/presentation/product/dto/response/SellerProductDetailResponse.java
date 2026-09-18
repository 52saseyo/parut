package com.parut.product.product.presentation.product.dto.response;

import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.domain.product.SaleUnit;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.enums.StockStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SellerProductDetailResponse(
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
        ProductStatus status,
        UUID stockId,
        int totalQuantity,
        int availableQuantity,
        int lowStockThreshold,
        StockStatus stockStatus,
        String url
) {

    public static SellerProductDetailResponse from(
            Product product,
            ProductStock stock,
            String imageUrl
    ) {
        return new SellerProductDetailResponse(
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
                product.getStatus(),
                stock.getId(),
                stock.getTotalQuantity(),
                stock.getAvailableQuantity(),
                stock.getLowStockThreshold(),
                stock.getStatus(),
                imageUrl
        );
    }
}
