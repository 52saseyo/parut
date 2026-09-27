package com.parut.product.timedeal.infrastructure.redis;

public final class TimeDealScheduleClaimLuaScript {

    public static final String CLAIM_SCRIPT = """
            -- KEYS[1]: 아직 선점되지 않은 실행 대기 Sorted Set
            -- KEYS[2]: 현재 처리 중인 작업과 lease 만료 시각을 저장하는 Sorted Set
            local scheduleKey = KEYS[1]
            local processingKey = KEYS[2]

            -- ARGV는 Java에서 전달한 문자열이므로 숫자로 변환한다.
            -- now: 현재 시각, leaseUntil: 선점 만료 시각, limit: 최대 선점 개수
            local now = tonumber(ARGV[1])
            local leaseUntil = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])

            -- 선점한 서버가 장애로 종료된 작업을 찾는다.
            -- processing의 score는 예약 시각이 아니라 lease 만료 시각이다.
            local expired = redis.call('ZRANGEBYSCORE', processingKey, '-inf', now)
            for _, timeDealId in ipairs(expired) do
                -- 만료 작업을 처리 중 목록에서 제거하고 즉시 재시도 대상으로 복구한다.
                redis.call('ZREM', processingKey, timeDealId) -- ZREM: 은 해당값을 지우는 것 고로 만료된 processingKey인 timedeal 를 제거하고 아래 scheduleKey로 등록한다
                redis.call('ZADD', scheduleKey, now, timeDealId)
            end

            -- 실행 시각이 현재까지 도달한 작업을 최대 limit개 조회한다.
            local due = redis.call('ZRANGEBYSCORE', scheduleKey, '-inf', now, 'LIMIT', 0, limit)
            for _, timeDealId in ipairs(due) do
                -- 조회한 작업을 대기 목록에서 제거하고 처리 중 목록으로 이동한다.
                -- 이 Script 전체가 원자적으로 실행되므로 다른 서버가 같은 작업을 가져갈 수 없다.
                redis.call('ZREM', scheduleKey, timeDealId)
                redis.call('ZADD', processingKey, leaseUntil, timeDealId)
            end

            -- Java가 DB 처리를 수행할 수 있도록 이번에 선점한 ID 목록을 반환한다.
            return due
            """;

    private TimeDealScheduleClaimLuaScript() {
    }
}
