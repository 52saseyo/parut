package com.parut.product.timedeal.presentation.dto.timedeal.response;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopResult;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.util.UUID;

public record TimeDealStopResponse(UUID timeDealId, TimeDealStatus status) {

    public static TimeDealStopResponse from(TimeDealStopResult result) {
        return new TimeDealStopResponse(result.timeDealId(), result.status());
    }
}
