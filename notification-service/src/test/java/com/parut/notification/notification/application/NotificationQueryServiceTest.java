package com.parut.notification.notification.application;

import com.parut.notification.notification.application.authorization.NotificationAuthorizationChecker;
import com.parut.notification.notification.domain.Notification;
import com.parut.notification.notification.domain.NotificationType;
import com.parut.notification.notification.infrastructure.persistence.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationQueryServiceTest {

    private NotificationRepository repository;
    private NotificationQueryService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        service = new NotificationQueryService(repository, new NotificationAuthorizationChecker());
    }

    @Test
    void firstPageUsesExtraRowToBuildNextCursor() {
        Instant newestTime = Instant.parse("2026-09-22T10:00:00Z");
        Instant lastTime = Instant.parse("2026-09-22T09:00:00Z");
        UUID newestId = UUID.randomUUID();
        UUID lastId = UUID.randomUUID();
        var first = notification(newestId, newestTime);
        var second = notification(lastId, lastTime);
        var extra = notification(UUID.randomUUID(), Instant.parse("2026-09-22T08:00:00Z"));
        when(repository.findFirstList(eq(userId), eq(false), eq(NotificationType.TIME_DEAL_OPENING_SOON), any(Pageable.class)))
                .thenReturn(List.of(first, second, extra));

        var result = service.getNotifications(
                userId, "CUSTOMER", null, null, 2, false, NotificationType.TIME_DEAL_OPENING_SOON);

        assertEquals(2, result.content().size());
        assertTrue(result.hasNext());
        assertEquals(lastTime, result.nextCursorCreatedAt());
        assertEquals(lastId, result.nextCursorId());
        verify(repository).findFirstList(eq(userId), eq(false), eq(NotificationType.TIME_DEAL_OPENING_SOON),
                argThat(pageable -> pageable.getPageSize() == 3));
    }

    @Test
    void nextPagePassesCursorAndClearsItOnLastPage() {
        Instant cursor = Instant.parse("2026-09-22T09:00:00Z");
        UUID cursorId = UUID.randomUUID();
        var one = notification(UUID.randomUUID(), Instant.parse("2026-09-22T08:00:00Z"));
        when(repository.findNextList(eq(userId), isNull(), isNull(), eq(cursor), eq(cursorId), any(Pageable.class)))
                .thenReturn(List.of(one));

        var result = service.getNotifications(userId, "SELLER", cursor, cursorId, 10, null, null);

        assertEquals(1, result.content().size());
        assertFalse(result.hasNext());
        assertNull(result.nextCursorCreatedAt());
        assertNull(result.nextCursorId());
    }

    @Test
    void unreadCountIsScopedToUser() {
        when(repository.countByUserIdAndIsReadFalse(userId)).thenReturn(3L);

        assertEquals(3L, service.getUnreadCount(userId, "ADMIN"));
        verify(repository).countByUserIdAndIsReadFalse(userId);
    }

    private Notification notification(UUID id, Instant createdAt) {
        Notification result = mock(Notification.class);
        when(result.getId()).thenReturn(id);
        when(result.getCreatedAt()).thenReturn(createdAt);
        return result;
    }
}
