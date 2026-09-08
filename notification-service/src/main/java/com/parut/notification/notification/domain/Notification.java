package com.parut.notification.notification.domain;

import java.time.Instant;
import java.util.UUID;

import com.parut.notification.global.common.entity.UpdatableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends UpdatableEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private ReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "read_at")
    private Instant readAt;

    public static Notification create(
            UUID eventId,
            String traceId,
            UUID userId,
            NotificationType type,
            String title,
            String content,
            ReferenceType referenceType,
            UUID referenceId
    ) {
        return new Notification(eventId, traceId, userId, type, title, content, referenceType, referenceId);
    }

    private Notification(
            UUID eventId,
            String traceId,
            UUID userId,
            NotificationType type,
            String title,
            String content,
            ReferenceType referenceType,
            UUID referenceId
    ) {
        if (eventId == null) {
            throw new IllegalArgumentException("이벤트 ID는 필수입니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (type == null) {
            throw new IllegalArgumentException("알림 유형은 필수입니다.");
        }
        if (referenceType == null) {
            throw new IllegalArgumentException("참조 유형은 필수입니다.");
        }
        if (referenceId == null) {
            throw new IllegalArgumentException("참조 ID는 필수입니다.");
        }
        if (traceId != null && traceId.length() > 100) {
            throw new IllegalArgumentException("추적 ID는 100자를 초과할 수 없습니다.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("알림 제목은 필수입니다.");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("알림 제목은 100자를 초과할 수 없습니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("알림 내용은 필수입니다.");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("알림 내용은 1000자를 초과할 수 없습니다.");
        }

        this.eventId = eventId;
        this.traceId = traceId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.isRead = false;
    }

    public void read(Instant readAt) {
        if (isRead) {
            return;
        }
        if (readAt == null) {
            throw new IllegalArgumentException("알림 읽음 시각은 필수입니다.");
        }

        this.isRead = true;
        this.readAt = readAt;
    }
}
