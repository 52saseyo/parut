package com.parut.product.product.presentation.stock.dto.request;


import com.parut.product.product.application.stock.dto.ProductStockItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ProductStockConfirmRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotEmpty(message = "확정 항목은 1개 이상이어야 합니다.")
        @Valid
        List<Item> items
) {
        public record Item(
                @NotNull(message = "상품 ID는 필수입니다.")
                UUID productId,

                @NotNull(message = "주문 항목 ID는 필수입니다.")
                UUID orderItemId
        ) {
                public ProductStockItem toItem() {
                        return new ProductStockItem(productId, orderItemId);
                }
        }

        public List<ProductStockItem> toItems() {
                return items.stream().map(Item::toItem).toList();
        }
}