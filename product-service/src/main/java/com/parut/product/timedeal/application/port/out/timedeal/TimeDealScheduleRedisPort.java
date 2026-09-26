package com.parut.product.timedeal.application.port.out.timedeal;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface TimeDealScheduleRedisPort {

    void schedule(UUID timeDealId, Instant startAt, Instant endAt);

    void remove(UUID timeDealId);

    List<UUID> claimOpenDue(Instant now, int limit, Duration lease);

    List<UUID> claimCloseDue(Instant now, int limit, Duration lease);

    void acknowledgeOpen(UUID timeDealId);

    void acknowledgeClose(UUID timeDealId);
}
