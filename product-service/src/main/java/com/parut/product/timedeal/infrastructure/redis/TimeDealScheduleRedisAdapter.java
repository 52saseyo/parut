package com.parut.product.timedeal.infrastructure.redis;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealScheduleRedisPort;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeDealScheduleRedisAdapter implements TimeDealScheduleRedisPort {

    private final RedissonClient redissonClient;

    @Override
    public void schedule(UUID timeDealId, Instant startAt, Instant endAt) {
        openSchedule().add(startAt.toEpochMilli(), timeDealId.toString());
        closeSchedule().add(endAt.toEpochMilli(), timeDealId.toString());
    }

    @Override
    public void remove(UUID timeDealId) {
        openSchedule().remove(timeDealId.toString());
        closeSchedule().remove(timeDealId.toString());
    }

    private RScoredSortedSet<String> openSchedule() {
        return redissonClient.getScoredSortedSet(TimeDealRedisKeys.openSchedule(), StringCodec.INSTANCE);
    }

    private RScoredSortedSet<String> closeSchedule() {
        return redissonClient.getScoredSortedSet(TimeDealRedisKeys.closeSchedule(), StringCodec.INSTANCE);
    }
}
