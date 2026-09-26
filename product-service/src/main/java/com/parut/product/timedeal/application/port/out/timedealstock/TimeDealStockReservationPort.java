package com.parut.product.timedeal.application.port.out.timedealstock;

import java.util.UUID;

public interface TimeDealStockReservationPort {

    TimeDealStockRestoreResult restore(UUID timeDealId, UUID stockId, UUID orderId, UUID taskId, int quantity);

    TimeDealStockReservationResult reserve(
            UUID timeDealId,
            UUID stockId,
            UUID orderId,
            int quantity
    );

    TimeDealStockCompensationResult compensate(
            UUID timeDealId,
            UUID stockId,
            UUID orderId
    );
}
