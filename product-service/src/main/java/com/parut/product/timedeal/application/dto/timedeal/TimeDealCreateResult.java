package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.time.Instant;
import java.util.UUID;


// NOTE: 직접 등록이면 productId가 null이다 — 전환으로 만든 타임딜에서만 출처 상품이 채워진다.
public record TimeDealCreateResult(
        UUID timeDealId,
        UUID productId,
        TimeDealStatus status,
        Instant startAt,
        Instant endAt,
        Instant createdAt
) {
    public static TimeDealCreateResult from(TimeDeal timeDeal) {
        return new TimeDealCreateResult(
                timeDeal.getId(),
                timeDeal.getProductId(),
                timeDeal.getStatus(),
                timeDeal.getStartAt(),
                timeDeal.getEndAt(),
                timeDeal.getCreatedAt()
        );
    }
}