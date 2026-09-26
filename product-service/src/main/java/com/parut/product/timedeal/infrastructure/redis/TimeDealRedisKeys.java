package com.parut.product.timedeal.infrastructure.redis;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * 타임딜 Redis Key 규칙을 한 곳에서 관리한다.
 *
 * <p>Redis는 임시 선점·동시성 제어 상태를 저장하고, 구매 이력의 최종 원본은 DB가 담당한다.</p>
 */
public final class TimeDealRedisKeys {

    private static final String STOCK_PREFIX = "timedeal:stock:";
    private static final String RESERVATION_PREFIX = "timedeal:reservation:";
    private static final Duration RESERVATION_KEY_TTL = Duration.ofMinutes(20);

    private TimeDealRedisKeys() {
    }

    public static String stock(UUID timeDealId, UUID stockId) {
        return STOCK_PREFIX + required(timeDealId, "timeDealId") + ":" + required(stockId, "stockId");
    }

    public static String reservation(UUID timeDealId, UUID orderId) {
        return RESERVATION_PREFIX + required(timeDealId, "timeDealId") + ":" + required(orderId, "orderId");
    }

    public static Duration reservationKeyTtl() {
        return RESERVATION_KEY_TTL;
    }

    public static String restoreTask(UUID taskId) {
        return "timedeal:restore:" + required(taskId, "taskId");
    }

    private static UUID required(UUID value, String name) {
        return Objects.requireNonNull(value, name + " must not be null");
    }
}
