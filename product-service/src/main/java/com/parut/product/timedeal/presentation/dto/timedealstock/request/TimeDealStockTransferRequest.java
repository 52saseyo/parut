package com.parut.product.timedeal.presentation.dto.timedealstock.request;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockTransferCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TimeDealStockTransferRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        UUID productId,

        @NotNull(message = "재고 이동 수량은 필수입니다.")
        Integer quantity
) {

    public TimeDealStockTransferCommand toCommand(
            UUID timeDealId,
            UUID requesterId,
            String requesterRole
    ) {
        return new TimeDealStockTransferCommand(
                timeDealId, productId, quantity, requesterId, requesterRole);
    }
}
