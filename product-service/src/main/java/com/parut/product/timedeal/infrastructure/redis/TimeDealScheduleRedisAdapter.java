package com.parut.product.timedeal.infrastructure.redis;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealScheduleRedisPort;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
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
        openSchedule().remove(timeDealId.toString());
        closeSchedule().remove(timeDealId.toString());
        openProcessing().remove(timeDealId.toString());
        closeProcessing().remove(timeDealId.toString());
    }

    @Override
    public List<UUID> claimOpenDue(Instant now, int limit, Duration lease) {
        return claim(TimeDealRedisKeys.openSchedule(), TimeDealRedisKeys.openProcessing(), now, limit, lease);
    }

    @Override
    public List<UUID> claimCloseDue(Instant now, int limit, Duration lease) {
        return claim(TimeDealRedisKeys.closeSchedule(), TimeDealRedisKeys.closeProcessing(), now, limit, lease);
    }

    @Override
    public void acknowledgeOpen(UUID timeDealId) {
        openProcessing().remove(timeDealId.toString());
    }

    @Override
    public void acknowledgeClose(UUID timeDealId) {
        closeProcessing().remove(timeDealId.toString());
    }

    private RScoredSortedSet<String> openSchedule() {
        return redissonClient.getScoredSortedSet(TimeDealRedisKeys.openSchedule(), StringCodec.INSTANCE);
    }

    private RScoredSortedSet<String> closeSchedule() {
        return redissonClient.getScoredSortedSet(TimeDealRedisKeys.closeSchedule(), StringCodec.INSTANCE);
    }

    private RScoredSortedSet<String> openProcessing() {
        return redissonClient.getScoredSortedSet(TimeDealRedisKeys.openProcessing(), StringCodec.INSTANCE);
    }

    private RScoredSortedSet<String> closeProcessing() {
        return redissonClient.getScoredSortedSet(TimeDealRedisKeys.closeProcessing(), StringCodec.INSTANCE);
    }

    private List<UUID> claim(
            String scheduleKey,
            String processingKey,
            Instant now,
            int limit,
            Duration lease
    ) {
        List<Object> claimed = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                TimeDealScheduleClaimLuaScript.CLAIM_SCRIPT,
                RScript.ReturnType.LIST,
                List.of(scheduleKey, processingKey),
                String.valueOf(now.toEpochMilli()),
                String.valueOf(now.plus(lease).toEpochMilli()),
                String.valueOf(limit)
        );

        return claimed.stream()
                .map(String::valueOf)
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }
}
