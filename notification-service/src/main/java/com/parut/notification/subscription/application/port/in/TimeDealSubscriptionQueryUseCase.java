package com.parut.notification.subscription.application.port.in;

import com.parut.notification.subscription.application.dto.TimeDealSubscriptionResult;

import java.util.List;
import java.util.UUID;

public interface TimeDealSubscriptionQueryUseCase {
    TimeDealSubscriptionResult getSubscriptionStatus(UUID userId, String requesterRole, UUID timeDealId);

    List<UUID> getActiveSubscriberIds(UUID timeDealId);
}
