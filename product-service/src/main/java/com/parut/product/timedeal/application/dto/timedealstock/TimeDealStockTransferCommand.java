package com.parut.product.timedeal.application.dto.timedealstock;

import java.util.UUID;

public record TimeDealStockTransferCommand(
        UUID timeDealId,
        UUID productId,
        Integer quantity,
        UUID requesterId,
        String requesterRole
) {
}
