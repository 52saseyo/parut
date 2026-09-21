package com.parut.notification.subscription.application.port.in;

import com.parut.notification.subscription.application.dto.SubscribeTimeDealCommand;
import com.parut.notification.subscription.application.dto.TimeDealSubscriptionResult;
import java.util.UUID;

public interface TimeDealSubscriptionCommandUseCase {
    TimeDealSubscriptionResult subscribe(SubscribeTimeDealCommand command);

    void unsubscribe(UUID userId, String requesterRole, UUID timeDealId);
}
