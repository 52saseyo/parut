package com.parut.notification.notification.application.port.in;

import com.parut.notification.notification.application.dto.NotificationCursorResult;
import com.parut.notification.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public interface NotificationQueryUseCase {
    NotificationCursorResult getNotifications(
            UUID userId,
            String requesterRole,
            Instant cursor,
            UUID cursorId,
            int size,
            Boolean isRead,
            NotificationType type
    );

    long getUnreadCount(UUID userId, String requesterRole);
}
