package com.parut.notification.notification.infrastructure.persistence;

import com.parut.notification.notification.domain.Notification;

import java.time.Instant;
import java.util.*;

import com.parut.notification.notification.domain.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * 동일한 이벤트로 이미 알림이 생성된 사용자 조회
     */
    @Query("""
        select notification.userId
          from Notification notification
         where notification.eventId = :eventId
           and notification.userId in :userIds
        """)
    Set<UUID> findExistingUserIds(
            @Param("eventId") UUID eventId,
            @Param("userIds") Collection<UUID> userIds
    );


    /**
     * 알림 목록 첫 페이지 조회
     */
    @Query("""
        select notification
          from Notification notification
         where notification.userId = :userId
           and (
                cast(:isRead as boolean) is null
                or notification.isRead = :isRead
           )
           and (
                cast(:type as string) is null
                or notification.type = :type
           )
         order by notification.createdAt desc,
                  notification.id desc
        """)
    List<Notification> findFirstList(
            @Param("userId") UUID userId,
            @Param("isRead") Boolean isRead,
            @Param("type") NotificationType type,
            Pageable pageable
    );



    /**
     * 알림 목록 다음 페이지 조회
     * createdAt이 같을 수 있으므로 id를 보조 커서로 사용
     */
    @Query("""
        select notification
          from Notification notification
         where notification.userId = :userId
           and (
                cast(:isRead as boolean) is null
                or notification.isRead = :isRead
           )
           and (
                cast(:type as string) is null
                or notification.type = :type
           )
           and (
                notification.createdAt < :cursorCreatedAt
                or (
                    notification.createdAt = :cursorCreatedAt
                    and notification.id < :cursorId
                )
           )
         order by notification.createdAt desc,
                  notification.id desc
        """)
    List<Notification> findNextList(
            @Param("userId") UUID userId,
            @Param("isRead") Boolean isRead,
            @Param("type") NotificationType type,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );


    /**
     * 본인 알림인지 확인하면서 단건 조회
     */
    Optional<Notification> findByIdAndUserId(UUID notificationId, UUID userId);

    /**
     * 알림 미읽음 개수 조회
     */
    long countByUserIdAndIsReadFalse(UUID userId);

}
