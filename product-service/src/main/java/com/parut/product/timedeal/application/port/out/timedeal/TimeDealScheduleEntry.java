package com.parut.product.timedeal.application.port.out.timedeal;

import java.time.Instant;
import java.util.UUID;

/**
 * Redis 판매 기간 예약 복구에 필요한 최소 정보다.
 */
public record TimeDealScheduleEntry(
        UUID timeDealId,
        Instant startAt,
        Instant endAt
) {
}
