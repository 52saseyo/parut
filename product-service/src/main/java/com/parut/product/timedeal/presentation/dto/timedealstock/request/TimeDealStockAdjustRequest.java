package com.parut.product.timedeal.presentation.dto.timedealstock.request;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TimeDealStockAdjustRequest(

        // 양수는 재고 추가, 음수는 재고 회수로 해석한다.
        @NotNull(message = "재고 조정 수량은 필수입니다.")
        Integer quantity
) {

    public TimeDealStockAdjustCommand toCommand(UUID timeDealId, UUID requesterId, String requesterRole) {
        return new TimeDealStockAdjustCommand(timeDealId, quantity, requesterId, requesterRole);
    }
}
