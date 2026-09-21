package com.parut.notification.notification.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotificationCursorResult(
        List<NotificationView> content,
        Instant nextCursorCreatedAt,
        UUID nextCursorId,
        boolean hasNext
) {
}
