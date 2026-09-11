package com.parut.product.product.presentation.stock.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductStockUpdateRequest(
        @NotNull(message = "총 재고 수량은 필수입니다.")
        @Min(value = 0, message = "총 재고 수량은 0 이상이어야 합니다.")
        Integer totalQuantity
) {
}