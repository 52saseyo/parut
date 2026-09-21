package com.parut.notification.notification.infrastructure.messaging;


import com.parut.notification.notification.application.dto.TimeDealOpeningSoonCommand;

import java.time.Instant;
import java.util.UUID;

public record TimeDealOpeningSoonEvent(
        UUID eventId,
        UUID timeDealId,
        String name,
        Instant startAt
) {
    public TimeDealOpeningSoonCommand toCommand(String traceId) {
        return new TimeDealOpeningSoonCommand(eventId, traceId, timeDealId, name, startAt);
    }
}
