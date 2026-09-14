package com.parut.product.timedeal.presentation.dto.timedeal.response;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealUpdateResult;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.time.Instant;
import java.util.UUID;

public record TimeDealUpdateResponse(UUID timeDealId, TimeDealStatus status, Instant updatedAt) {
    public static TimeDealUpdateResponse from(TimeDealUpdateResult result) {
        return new TimeDealUpdateResponse(result.timeDealId(), result.status(), result.updatedAt());
    }
}
