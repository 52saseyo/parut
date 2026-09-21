package com.parut.notification.subscription.application.dto;

import java.util.UUID;

public record TimeDealSubscriptionResult(
        UUID timeDealId,
        boolean subscribed
) {
}
