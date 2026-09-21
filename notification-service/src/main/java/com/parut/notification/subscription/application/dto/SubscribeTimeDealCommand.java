package com.parut.notification.subscription.application.dto;

import java.util.UUID;

public record SubscribeTimeDealCommand(
        UUID userId,
        String requesterRole,
        UUID timeDealId
) {
}
