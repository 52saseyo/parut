package com.parut.product.timedeal.application.port.out.timedealstock;

import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;

import java.util.Optional;
import java.util.UUID;

public interface TimeDealStockRepository {

    Optional<TimeDealStock> findByTimeDealId(UUID timeDealId);

    TimeDealStock save(TimeDealStock timeDealStock);
}