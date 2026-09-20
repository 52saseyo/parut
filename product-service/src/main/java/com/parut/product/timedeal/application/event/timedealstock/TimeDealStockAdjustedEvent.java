package com.parut.product.timedeal.application.event.timedealstock;

import java.util.UUID;

public record TimeDealStockAdjustedEvent(
        UUID timeDealId,
        UUID stockId,
        Integer availableQuantity
) {
}
