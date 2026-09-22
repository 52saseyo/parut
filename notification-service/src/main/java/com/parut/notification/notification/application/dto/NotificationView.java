package com.parut.notification.notification.application.dto;

import com.parut.notification.notification.domain.Notification;
import com.parut.notification.notification.domain.NotificationType;
import com.parut.notification.notification.domain.ReferenceType;

import java.time.Instant;
import java.util.UUID;

public record NotificationView(
        UUID notificationId,
        NotificationType type,
        String title,
        String content,
        ReferenceType referenceType,
        UUID referenceId,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationView from(
            Notification notification
    ) {
        return new NotificationView(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
