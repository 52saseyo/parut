package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.util.UUID;

public record TimeDealStopResult(UUID timeDealId, TimeDealStatus status) {

    public static TimeDealStopResult from(TimeDeal timeDeal) {
        return new TimeDealStopResult(timeDeal.getId(), timeDeal.getStatus());
    }
}
