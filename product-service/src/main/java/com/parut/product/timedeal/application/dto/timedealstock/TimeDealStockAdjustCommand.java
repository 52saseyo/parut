package com.parut.product.timedeal.application.dto.timedealstock;

import java.util.UUID;

public record TimeDealStockAdjustCommand(
        UUID timeDealId,
        Integer quantity,
        UUID requesterId,
        String requesterRole
) {
}
