package com.parut.product.product.presentation.stock.dto.request;


import com.parut.product.product.application.stock.dto.ProductStockReserveItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


import java.util.List;
import java.util.UUID;

public record ProductStockReserveRequest (
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotEmpty(message = "예약 항목은 1개 이상이어야 합니다.")
        @Valid
        List<Item> items
) {
        public record Item(
                @NotNull(message = "상품 ID는 필수입니다.")
                UUID productId,

                @NotNull(message = "주문 항목 ID는 필수입니다.")
                UUID orderItemId,

                @NotNull(message = "수량은 필수입니다.")
                @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
                Integer quantity
        ) {
                public ProductStockReserveItem toItem() {
                        return new ProductStockReserveItem(productId, orderItemId, quantity);
                }
        }

        public List<ProductStockReserveItem> toItems() {
                return items.stream().map(Item::toItem).toList();
        }
}