package com.parut.notification.notification.application.port.in;

import java.util.UUID;

public interface NotificationCommandUseCase {
    void read(
            UUID userId,
            String requesterRole,
            UUID notificationId
    );
}
