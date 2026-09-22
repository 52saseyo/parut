package com.parut.product.timedeal.application.dto.timedeal;

import java.time.Instant;
import java.util.UUID;

public record TimeDealOpeningSoonTarget(
        UUID timeDealId,
        String timeDealName,
        Instant timeDealStartAt
) {
}
