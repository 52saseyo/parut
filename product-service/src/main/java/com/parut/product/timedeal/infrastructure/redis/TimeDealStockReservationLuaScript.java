package com.parut.product.timedeal.infrastructure.redis;

/**
 * 타임딜 재고 확인, 차감과 구매 선점 Key 저장을 하나의 Redis Script로 처리한다.
 * 실제 Redisson RScript 실행 연결은 다음 단계의 Adapter에서 담당한다.
 */
public final class TimeDealStockReservationLuaScript {

    public static final int SOLD_OUT = 0;
    public static final int RESERVED = 1;
    public static final int DUPLICATE_ORDER = 2;
    public static final int INVALID_QUANTITY = 3;
    public static final int STOCK_NOT_INITIALIZED = 4;
    public static final int INSUFFICIENT_STOCK = 5;

    public static final int COMPENSATION_ALREADY_COMPLETED = 0;
    public static final int COMPENSATION_COMPLETED = 1;
    public static final int COMPENSATION_STOCK_NOT_INITIALIZED = 2;

    public static final String SCRIPT = """
            -- KEYS[1]: timedeal:stock:{timeDealId}:{stockId}
            -- KEYS[2]: timedeal:reservation:{timeDealId}:{orderId}
            -- ARGV[1]: reservation quantity
            -- ARGV[2]: reservation key TTL in seconds
            local quantity = tonumber(ARGV[1])

            -- 같은 orderId의 선점이 이미 있으면 재고를 다시 차감하지 않는다.
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return 2
            end

            -- 수량 검증은 Application에서도 수행하지만 Script에서도 방어한다.
            if quantity == nil or quantity <= 0 then
                return 3
            end

            local stockValue = redis.call('GET', KEYS[1])
            -- stock Key가 없으면 품절이 아니라 초기화·복구 문제다.
            if stockValue == false then
                return 4
            end

            local stock = tonumber(stockValue)
            if stock == nil then
                return 4
            end

            if stock == 0 then
                return 0
            end

            if stock < quantity then
                return 5
            end

            redis.call('DECRBY', KEYS[1], quantity)
            -- reservation Key에는 보상에 사용할 선점 수량을 저장한다.
            redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[2], 'NX')
            return 1
            """;

    /**
     * reservation Key의 수량만큼 stock Key를 복구하고 reservation Key를 삭제한다.
     * 두 작업을 하나의 Script로 묶어 중복 보상과 부분 보상을 방지한다.
     */
    public static final String COMPENSATION_SCRIPT = """
            -- KEYS[1]: timedeal:stock:{timeDealId}:{stockId}
            -- KEYS[2]: timedeal:reservation:{timeDealId}:{orderId}
            local reservationValue = redis.call('GET', KEYS[2])

            -- 이미 보상됐거나 TTL이 만료된 경우는 멱등 성공으로 처리한다.
            if reservationValue == false then
                return 0
            end

            local quantity = tonumber(reservationValue)
            local stockValue = redis.call('GET', KEYS[1])

            -- stock Key가 없으면 수량을 복구하지 않고 reservation Key도 보존한다.
            -- 이후 재처리 또는 정합성 복구 작업의 대상이 되어야 한다.
            if stockValue == false or tonumber(stockValue) == nil or quantity == nil or quantity <= 0 then
                return 2
            end

            redis.call('INCRBY', KEYS[1], quantity)
            redis.call('DEL', KEYS[2])
            return 1
            """;

    private TimeDealStockReservationLuaScript() {
    }
}
