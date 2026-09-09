package com.parut.product.timedeal.presentation.dto.timedealpurchase.request;

import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseReserveCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;


public record TimeDealPurchaseReserveRequest(

        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        Integer quantity
) {

    // NOTE: 변환을 Request에 두는 이유는 의존 방향이다 — Command가 Request를 알면 역전이다.
    public TimeDealPurchaseReserveCommand toCommand(UUID timeDealId, UUID userId) {
        return new TimeDealPurchaseReserveCommand(timeDealId, orderId, userId, quantity);
    }
}
