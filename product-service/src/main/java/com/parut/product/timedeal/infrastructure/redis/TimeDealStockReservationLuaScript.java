package com.parut.product.timedeal.infrastructure.redis;

/**
 * 타임딜 재고 확인, 차감과 구매 선점 Key 저장을 하나의 Redis Script로 처리한다.
 * 실제 Redisson RScript 실행 연결은 다음 단계의 Adapter에서 담당한다.
 */
public final class TimeDealStockReservationLuaScript {

    public static final int SOLD_OUT = 0;
    public static final int RESERVED = 1;
    public static final int DUPLICATE_ORDER = 2;

    public static final String SCRIPT = """
            -- KEYS[1]: timedeal:stock:{timeDealId}:{stockId}
            -- KEYS[2]: timedeal:reservation:{timeDealId}:{orderId}
            -- ARGV[1]: reservation quantity
            -- ARGV[2]: reservation key TTL in seconds
            local quantity = tonumber(ARGV[1])
            local stock = tonumber(redis.call('GET', KEYS[1]) or '0')

            -- 같은 orderId의 선점이 이미 있으면 재고를 다시 차감하지 않는다.
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return 2
            end

            -- 수량 검증은 Application에서도 수행하지만 Script에서도 방어한다.
            if quantity == nil or quantity <= 0 or stock < quantity then
                return 0
            end

            redis.call('DECRBY', KEYS[1], quantity)
            -- reservation Key에는 보상에 사용할 선점 수량을 저장한다.
            redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[2], 'NX')
            return 1
            """;

    private TimeDealStockReservationLuaScript() {
    }
}
