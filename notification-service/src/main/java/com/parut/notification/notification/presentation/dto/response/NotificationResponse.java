package com.parut.notification.notification.presentation.dto.response;

import com.parut.notification.notification.application.dto.NotificationView;
import com.parut.notification.notification.domain.NotificationType;
import com.parut.notification.notification.domain.ReferenceType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
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
    public static NotificationResponse from(
            NotificationView view
    ) {
        return new NotificationResponse(
                view.notificationId(),
                view.type(),
                view.title(),
                view.content(),
                view.referenceType(),
                view.referenceId(),
                view.read(),
                view.readAt(),
                view.createdAt()
        );
    }
}
