package com.parut.notification.notification.application;

import com.parut.notification.global.exception.BusinessException;
import com.parut.notification.global.exception.ErrorCode;
import com.parut.notification.notification.application.authorization.NotificationAuthorizationChecker;
import com.parut.notification.notification.domain.Notification;
import com.parut.notification.notification.domain.NotificationType;
import com.parut.notification.notification.domain.ReferenceType;
import com.parut.notification.notification.infrastructure.persistence.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationCommandServiceTest {

    private NotificationRepository repository;
    private NotificationCommandService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID notificationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        service = new NotificationCommandService(repository, new NotificationAuthorizationChecker());
    }

    @Test
    void readMarksOwnNotificationOnlyOnce() {
        var notification = Notification.create(
                UUID.randomUUID(), "trace", userId, NotificationType.TIME_DEAL_OPENING_SOON,
                "타임딜 오픈 임박", "곧 시작됩니다.", ReferenceType.TIME_DEAL, UUID.randomUUID());
        when(repository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.of(notification));

        service.read(userId, "CUSTOMER", notificationId);
        var firstReadAt = notification.getReadAt();
        service.read(userId, "CUSTOMER", notificationId);

        assertTrue(notification.isRead());
        assertNotNull(firstReadAt);
        assertEquals(firstReadAt, notification.getReadAt());
        verify(repository, never()).save(any());
    }

    @Test
    void missingOrOtherUsersNotificationReturnsNotFound() {
        when(repository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.empty());

        var error = assertThrows(BusinessException.class,
                () -> service.read(userId, "CUSTOMER", notificationId));

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, error.getErrorCode());
        verify(repository).findByIdAndUserId(notificationId, userId);
    }
}
