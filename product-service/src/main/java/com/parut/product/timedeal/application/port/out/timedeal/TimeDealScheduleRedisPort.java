package com.parut.product.timedeal.application.port.out.timedeal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TimeDealScheduleRedisPort {

    void schedule(UUID timeDealId, Instant startAt, Instant endAt);

    void remove(UUID timeDealId);

    List<UUID> findOpenDue(Instant now, int limit);

    List<UUID> findCloseDue(Instant now, int limit);

    void removeOpen(UUID timeDealId);

    void removeClose(UUID timeDealId);
}
