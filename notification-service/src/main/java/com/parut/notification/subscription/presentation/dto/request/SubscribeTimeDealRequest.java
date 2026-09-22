package com.parut.notification.subscription.presentation.dto.request;

import com.parut.notification.subscription.application.dto.SubscribeTimeDealCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubscribeTimeDealRequest(
        @NotNull(message = "타임딜 ID는 필수입니다.") UUID timeDealId
) {
    public SubscribeTimeDealCommand toCommand(
            UUID userId,
            String requesterRole
    ) {
        return new SubscribeTimeDealCommand(
                userId,
                requesterRole,
                timeDealId
        );
    }
}
