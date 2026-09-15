package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.time.Instant;
import java.util.UUID;

public record TimeDealUpdateResult(UUID timeDealId, TimeDealStatus status, Instant updatedAt) {
    public static TimeDealUpdateResult from(TimeDeal timeDeal) {
        return new TimeDealUpdateResult(timeDeal.getId(), timeDeal.getStatus(), timeDeal.getUpdatedAt());
    }
}
