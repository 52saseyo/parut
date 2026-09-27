package com.parut.product.timedeal.application.event.timedeal;

import java.time.Instant;
import java.util.UUID;

public record TimeDealScheduleUpdatedEvent(
        UUID timeDealId,
        Instant startAt,
        Instant endAt
) {
}
