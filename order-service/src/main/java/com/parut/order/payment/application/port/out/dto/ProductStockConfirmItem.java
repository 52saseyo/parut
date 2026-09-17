package com.parut.order.payment.application.port.out.dto;

import java.util.UUID;

public record ProductStockConfirmItem(
        UUID productId,
        UUID orderItemId
) {
}
