package com.parut.notification.notification.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TimeDealOpeningSoonCommand(
        UUID eventId,
        String traceId,
        UUID timeDealId,
        String timeDealName,
        Instant timeDealStartAt
) {
}
