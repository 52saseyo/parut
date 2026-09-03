package com.parut.product.product.presentation.stock.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProductStockRestoreRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotNull(message = "주문 항목 ID는 필수입니다.")
        UUID orderItemId
) {
}