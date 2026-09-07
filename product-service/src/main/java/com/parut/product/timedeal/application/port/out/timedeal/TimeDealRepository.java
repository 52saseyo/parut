package com.parut.product.timedeal.application.port.out.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDeal;

import java.util.Optional;
import java.util.UUID;

public interface TimeDealRepository {

    Optional<TimeDeal> findById(UUID timeDealId);

    TimeDeal save(TimeDeal timeDeal);
}