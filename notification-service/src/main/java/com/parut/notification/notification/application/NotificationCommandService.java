package com.parut.notification.notification.application;

import com.parut.notification.global.exception.BusinessException;
import com.parut.notification.global.exception.ErrorCode;
import com.parut.notification.notification.application.authorization.NotificationAuthorizationChecker;
import com.parut.notification.notification.application.port.in.NotificationCommandUseCase;
import com.parut.notification.notification.domain.Notification;
import com.parut.notification.notification.infrastructure.persistence.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class NotificationCommandService implements NotificationCommandUseCase {
    private final NotificationRepository notificationRepository;
    private final NotificationAuthorizationChecker authorizationChecker;



    @Override
    @Transactional
    public void read(UUID userId, String requesterRole, UUID notificationId) {
        authorizationChecker.requireNotificationAccess(requesterRole);
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.read(Instant.now());
    }
}
