package com.parut.notification.notification.application;

import com.parut.notification.notification.application.dto.TimeDealOpeningSoonCommand;
import com.parut.notification.notification.application.port.in.TimeDealOpeningSoonNotificationUseCase;
import com.parut.notification.notification.domain.Notification;
import com.parut.notification.notification.domain.NotificationType;
import com.parut.notification.notification.domain.ReferenceType;
import com.parut.notification.notification.infrastructure.persistence.NotificationRepository;
import com.parut.notification.subscription.application.port.in.TimeDealSubscriptionQueryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeDealOpeningSoonNotificationService implements TimeDealOpeningSoonNotificationUseCase {

    private static final ZoneId SERVICE_ZONE =  ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter START_AT_FORMATTER = DateTimeFormatter.ofPattern("M월 d일 HH:mm");

    private final TimeDealSubscriptionQueryUseCase subscriptionQueryUseCase;
    private final NotificationRepository notificationRepository;


    @Override
    @Transactional
    public void createNotifications(TimeDealOpeningSoonCommand command) {

        List<UUID> subscriberIds = subscriptionQueryUseCase.getActiveSubscriberIds(command.timeDealId());

        if (subscriberIds.isEmpty()) {
            log.info(
                    "[TimeDealOpeningSoonNotificationService] 활성 구독자 없음 "
                            + "eventId={}, timeDealId={}",
                    command.eventId(),
                    command.timeDealId()
            );
            return;
        }

        Set<UUID> existingUserIds = notificationRepository.findExistingUserIds(command.eventId(), subscriberIds);

        List<Notification> notifications = subscriberIds.stream()
                .filter(userId -> !existingUserIds.contains(userId))
                .map(userId -> createNotification(command, userId))
                .toList();

        if(notifications.isEmpty()) {
            log.info(
                    "[TimeDealOpeningSoonNotificationService] 생성할 신규 알림 없음 "
                            + "eventId={}, timeDealId={}",
                    command.eventId(),
                    command.timeDealId()
            );
            return;
        }

        notificationRepository.saveAll(notifications);

        log.info(
                "[TimeDealOpeningSoonNotificationService] 알림 생성 완료 "
                        + "eventId={}, timeDealId={}, count={}",
                command.eventId(),
                command.timeDealId(),
                notifications.size()
        );
    }

    private Notification createNotification(TimeDealOpeningSoonCommand command, UUID userId) {
        String formattedStartAt = command
                .timeDealStartAt()
                .atZone(SERVICE_ZONE)
                .format(START_AT_FORMATTER);

        String content =
                "%s 타임딜이 %s에 시작됩니다."
                        .formatted(
                                command.timeDealName(),
                                formattedStartAt
                        );

        return Notification.create(
                command.eventId(),
                command.traceId(),
                userId,
                NotificationType.TIME_DEAL_OPENING_SOON,
                "타임딜 오픈 임박",
                content,
                ReferenceType.TIME_DEAL,
                command.timeDealId()
        );
    }
}
