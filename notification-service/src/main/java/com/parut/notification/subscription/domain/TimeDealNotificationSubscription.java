package com.parut.notification.subscription.domain;

import java.time.Instant;
import java.util.UUID;

import com.parut.notification.global.common.entity.UpdatableEntity;

import com.parut.notification.global.exception.BusinessException;
import com.parut.notification.global.exception.ErrorCode;
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
            throw new BusinessException(ErrorCode.NOTIFICATION_SUBSCRIPTION_USER_ID_REQUIRED);
        }
        if (timeDealId == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_SUBSCRIPTION_TIME_DEAL_ID_REQUIRED);
        }

        this.userId = userId;
        this.timeDealId = timeDealId;
    }

    public void cancel(Instant deletedAt) {
        if (this.deletedAt != null) {
            return;
        }
        if (deletedAt == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_SUBSCRIPTION_CANCEL_TIME_REQUIRED);
        }

        this.deletedAt = deletedAt;
    }

    public void resubscribe() {
        if (deletedAt == null) {
            return;
        }

        this.deletedAt = null;
    }

    public boolean isSubscribed() {
        return deletedAt == null;
    }
}
