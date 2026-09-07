package com.parut.product.product.presentation.product.dto.response;

import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.domain.product.SaleUnit;
import com.parut.product.product.domain.stock.entity.ProductStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductOrderInfoResponse(
        UUID productId,
        UUID stockId,
        UUID sellerId,
        String productName,

        AppearanceType appearanceType,
        String origin,
        LocalDate harvestDate,
        SaleUnit saleUnit,
        BigDecimal unitQuantity,

        Long originalPrice,
        ProductStatus saleStatus,
        boolean purchasable
) {
    public static ProductOrderInfoResponse from(Product product, ProductStock productStock, boolean purchasable) {

        return new ProductOrderInfoResponse(
                product.getId(),
                productStock.getId(),
                product.getSellerId(),
                product.getName(),

                product.getAppearanceType(),
                product.getOrigin(),
                product.getHarvestDate(),
                product.getSaleUnit(),
                product.getUnitQuantity(),

                product.getPrice(),
                product.getStatus(),
                purchasable
        );
    }
}
