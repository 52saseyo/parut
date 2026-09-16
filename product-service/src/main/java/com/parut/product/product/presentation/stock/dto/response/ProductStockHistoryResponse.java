package com.parut.product.product.presentation.stock.dto.response;

import com.parut.product.product.application.stock.dto.ProductStockHistoryResult;

import java.util.List;

public record ProductStockHistoryResponse(
    String productName,
    List<ProductStockHistoryItemResponse> items
  ) {
        public static ProductStockHistoryResponse from(ProductStockHistoryResult result) {
            return new ProductStockHistoryResponse(
                    result.productName(),
                    result.items().stream().map(ProductStockHistoryItemResponse::from).toList()
            );
        }
    }