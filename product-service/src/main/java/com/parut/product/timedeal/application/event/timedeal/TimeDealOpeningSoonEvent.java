package com.parut.product.timedeal.application.event.timedeal;

import java.time.Instant;
import java.util.UUID;

public record TimeDealOpeningSoonEvent(
        UUID eventId,
        UUID timeDealId,
        String timeDealName,
        Instant timeDealStartAt
) {
    public static final String EVENT_TYPE = "TIME_DEAL_OPENING_SOON";
}
