package com.parut.product.timedeal.application.port.out.timedeal;

import java.time.Instant;
import java.util.UUID;

public interface TimeDealScheduleRedisPort {

    void schedule(UUID timeDealId, Instant startAt, Instant endAt);

    void remove(UUID timeDealId);
}
