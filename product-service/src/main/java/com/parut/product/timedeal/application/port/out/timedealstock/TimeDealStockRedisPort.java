package com.parut.product.timedeal.application.port.out.timedealstock;

import java.util.UUID;

public interface TimeDealStockRedisPort {

    void setAvailableQuantity(UUID timeDealId, UUID stockId, int availableQuantity);

    boolean setAvailableQuantityIfAbsent(UUID timeDealId, UUID stockId, int availableQuantity);

    void delete(UUID timeDealId, UUID stockId);
}
