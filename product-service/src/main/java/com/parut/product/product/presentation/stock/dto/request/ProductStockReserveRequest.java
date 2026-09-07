package com.parut.product.product.presentation.stock.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


import java.util.UUID;

public record ProductStockReserveRequest (
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotNull(message = "주문 항목 ID는 필수입니다.")
        UUID orderItemId,

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message= "수량은 1 이상이어야 합니다.")
        Integer quantity
) {

}
