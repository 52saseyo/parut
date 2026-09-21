package com.parut.notification.subscription.presentation.dto.response;

import com.parut.notification.subscription.application.dto.TimeDealSubscriptionResult;

import java.util.UUID;

public record TimeDealSubscriptionResponse(
        UUID timeDealId,
        boolean subscribed
) {
    public static TimeDealSubscriptionResponse from(TimeDealSubscriptionResult result) {
        return new TimeDealSubscriptionResponse(
                result.timeDealId(),
                result.subscribed()
        );
    }
}
