package com.parut.notification.notification.domain;

import java.time.Instant;
import java.util.UUID;

import com.parut.notification.global.common.entity.UpdatableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_time_deal_notification_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeDealNotificationSubscription extends UpdatableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "time_deal_id", nullable = false)
    private UUID timeDealId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static TimeDealNotificationSubscription subscribe(UUID userId, UUID timeDealId) {
        return new TimeDealNotificationSubscription(userId, timeDealId);
    }

    private TimeDealNotificationSubscription(UUID userId, UUID timeDealId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (timeDealId == null) {
            throw new IllegalArgumentException("타임딜 ID는 필수입니다.");
        }

        this.userId = userId;
        this.timeDealId = timeDealId;
    }

    public void cancel(Instant deletedAt) {
        if (this.deletedAt != null) {
            return;
        }
        if (deletedAt == null) {
            throw new IllegalArgumentException("알림 신청 취소 시각은 필수입니다.");
        }

        this.deletedAt = deletedAt;
    }

    public void resubscribe() {
        if (deletedAt == null) {
            return;
        }

        this.deletedAt = null;
    }
}
