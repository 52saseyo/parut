package com.parut.product.timedeal.application.event.timedealstock;

import java.util.UUID;

public record TimeDealStockTransferredEvent(
        UUID timeDealId,
        UUID stockId,
        Integer availableQuantity
) {
}
