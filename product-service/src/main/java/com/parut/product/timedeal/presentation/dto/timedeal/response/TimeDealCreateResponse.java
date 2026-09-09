package com.parut.product.timedeal.presentation.dto.timedeal.response;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.time.Instant;
import java.util.UUID;


public record TimeDealCreateResponse(
        UUID timeDealId,
        UUID productId,
        TimeDealStatus status,
        Instant startAt,
        Instant endAt,
        Instant createdAt
) {
    public static TimeDealCreateResponse from(TimeDealCreateResult timeDealCreateResult) {
        return new TimeDealCreateResponse(
                timeDealCreateResult.timeDealId(),
                timeDealCreateResult.productId(),
                timeDealCreateResult.status(),
                timeDealCreateResult.startAt(),
                timeDealCreateResult.endAt(),
                timeDealCreateResult.createdAt()
        );
    }
}
