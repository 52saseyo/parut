package com.parut.product.timedeal.infrastructure.redis;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealScheduleRedisPort;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public void remove(UUID timeDealId) { // NOTE: 타임딜 삭제시 해당 redis open, close 모두 삭제
        removeOpen(timeDealId);
        removeClose(timeDealId);
    }

    @Override
    public List<UUID> findOpenDue(Instant now, int limit) {
        return findDue(openSchedule(), now, limit);
    }

    @Override
    public List<UUID> findCloseDue(Instant now, int limit) {
        return findDue(closeSchedule(), now, limit);
    }

    @Override
    public void removeOpen(UUID timeDealId) {
        openSchedule().remove(timeDealId.toString());
    }

    @Override
    public void removeClose(UUID timeDealId) {
        closeSchedule().remove(timeDealId.toString());
    }

    private RScoredSortedSet<String> openSchedule() {
        return redissonClient.getScoredSortedSet(TimeDealRedisKeys.openSchedule(), StringCodec.INSTANCE);
    }

    private RScoredSortedSet<String> closeSchedule() {
        return redissonClient.getScoredSortedSet(TimeDealRedisKeys.closeSchedule(), StringCodec.INSTANCE);
    }

    private List<UUID> findDue(RScoredSortedSet<String> schedule, Instant now, int limit) {
        return schedule.valueRange(0, true, now.toEpochMilli(), true, 0, limit)
                .stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }
}
