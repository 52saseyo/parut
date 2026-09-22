package com.parut.notification.notification.application;

import com.parut.notification.notification.application.authorization.NotificationAuthorizationChecker;
import com.parut.notification.notification.application.dto.NotificationCursorResult;
import com.parut.notification.notification.application.dto.NotificationView;
import com.parut.notification.notification.application.port.in.NotificationQueryUseCase;
import com.parut.notification.notification.domain.Notification;
import com.parut.notification.notification.domain.NotificationType;
import com.parut.notification.notification.infrastructure.persistence.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class NotificationQueryService implements NotificationQueryUseCase {

    private final NotificationRepository notificationRepository;
    private final NotificationAuthorizationChecker authorizationChecker;


    @Override
    public NotificationCursorResult getNotifications(
            UUID userId,
            String requesterRole,
            Instant cursor,
            UUID cursorId,
            int size,
            Boolean isRead,
            NotificationType type
    ) {
        authorizationChecker.requireNotificationAccess(requesterRole);
        Pageable pageable = PageRequest.of(0, size + 1);

        List<Notification> queriedNotifications;

        if (cursor == null) {
            queriedNotifications =
                    notificationRepository.findFirstList(
                            userId,
                            isRead,
                            type,
                            pageable
                    );
        } else {
            queriedNotifications =
                    notificationRepository.findNextList(
                            userId,
                            isRead,
                            type,
                            cursor,
                            cursorId,
                            pageable
                    );
        }
        boolean hasNext = queriedNotifications.size() > size;

        List<Notification> pageContent = hasNext ? queriedNotifications.subList(0, size) : queriedNotifications;

        List<NotificationView> content = pageContent.stream()
                .map(NotificationView::from)
                .toList();

        Notification lastNotification = pageContent.isEmpty()
                ? null
                : pageContent.get(pageContent.size() - 1);

        Instant nextCursorCreatedAt =
                hasNext && lastNotification != null
                        ? lastNotification.getCreatedAt()
                        : null;

        UUID nextCursorId =
                hasNext && lastNotification != null
                        ? lastNotification.getId()
                        : null;

        return new NotificationCursorResult(
                content,
                nextCursorCreatedAt,
                nextCursorId,
                hasNext
        );
    }

    @Override
    public long getUnreadCount(UUID userId, String requesterRole) {
        authorizationChecker.requireNotificationAccess(requesterRole);
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }





}
