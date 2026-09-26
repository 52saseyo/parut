package com.parut.product.timedeal.application.dto.timedealpurchase;

import java.util.UUID;

public record TimeDealStockRestorePayload(UUID timeDealId, UUID stockId, UUID orderId, int quantity) {
}
