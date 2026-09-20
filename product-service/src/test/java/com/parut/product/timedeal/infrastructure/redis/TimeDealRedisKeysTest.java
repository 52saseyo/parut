package com.parut.product.timedeal.infrastructure.redis;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDealRedisKeysTest {

    @Test
    void 재고와_선점_key를_규칙에_맞게_생성한다() {
        UUID timeDealId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID stockId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID orderId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        assertThat(TimeDealRedisKeys.stock(timeDealId, stockId))
                .isEqualTo("timedeal:stock:11111111-1111-1111-1111-111111111111:22222222-2222-2222-2222-222222222222");
        assertThat(TimeDealRedisKeys.reservation(timeDealId, orderId))
                .isEqualTo("timedeal:reservation:11111111-1111-1111-1111-111111111111:33333333-3333-3333-3333-333333333333");
    }

    @Test
    void reservation_key_ttl은_15분이다() {
        assertThat(TimeDealRedisKeys.reservationKeyTtl()).isEqualTo(Duration.ofMinutes(15));
    }
}
