package com.parut.product.timedeal.application.port.out.timedealstock;

import java.util.UUID;

public interface TimeDealStockReservationPort {

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
