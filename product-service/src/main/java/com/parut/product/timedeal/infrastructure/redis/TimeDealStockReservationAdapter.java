package com.parut.product.timedeal.infrastructure.redis;

import com.parut.product.timedeal.application.metrics.timedeal.TimeDealRedisMetrics;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockCompensationResult;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockReservationPort;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockReservationResult;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeDealStockReservationAdapter implements TimeDealStockReservationPort {

    private final RedissonClient redissonClient;
    private final TimeDealRedisMetrics metrics;

    @Override
    public TimeDealStockReservationResult reserve(
            UUID timeDealId,
            UUID stockId,
            UUID orderId,
            int quantity
    ) {
        String stockKey = TimeDealRedisKeys.stock(timeDealId, stockId);
        String reservationKey = TimeDealRedisKeys.reservation(timeDealId, orderId);
        long reservationTtlSeconds = TimeDealRedisKeys.reservationKeyTtl().toSeconds();

        Timer.Sample timer = metrics.startReservationScript();
        Long result;
        try {
            result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    TimeDealStockReservationLuaScript.SCRIPT,
                    RScript.ReturnType.LONG,
                    List.of(stockKey, reservationKey),
                    String.valueOf(quantity),
                    String.valueOf(reservationTtlSeconds)
            );
        } finally {
            metrics.stopReservationScript(timer);
        }

        if (result == null) {
            throw new IllegalStateException("Redis reservation script returned null");
        }

        TimeDealStockReservationResult reservationResult = switch (Math.toIntExact(result)) {
            case TimeDealStockReservationLuaScript.RESERVED -> TimeDealStockReservationResult.RESERVED;
            case TimeDealStockReservationLuaScript.SOLD_OUT -> TimeDealStockReservationResult.SOLD_OUT;
            case TimeDealStockReservationLuaScript.DUPLICATE_ORDER -> TimeDealStockReservationResult.DUPLICATE_ORDER;
            case TimeDealStockReservationLuaScript.INVALID_QUANTITY -> TimeDealStockReservationResult.INVALID_QUANTITY;
            case TimeDealStockReservationLuaScript.STOCK_NOT_INITIALIZED ->
                    TimeDealStockReservationResult.STOCK_NOT_INITIALIZED;
            case TimeDealStockReservationLuaScript.INSUFFICIENT_STOCK ->
                    TimeDealStockReservationResult.INSUFFICIENT_STOCK;
            default -> throw new IllegalStateException("Unknown Redis reservation result: " + result);
        };
        metrics.recordReservationResult(reservationResult);
        return reservationResult;
    }

    @Override
    public TimeDealStockCompensationResult compensate(
            UUID timeDealId,
            UUID stockId,
            UUID orderId
    ) {
        String stockKey = TimeDealRedisKeys.stock(timeDealId, stockId);
        String reservationKey = TimeDealRedisKeys.reservation(timeDealId, orderId);

        Timer.Sample timer = metrics.startCompensationScript();
        Long result;
        try {
            result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    TimeDealStockReservationLuaScript.COMPENSATION_SCRIPT,
                    RScript.ReturnType.LONG,
                    List.of(stockKey, reservationKey)
            );
        } finally {
            metrics.stopCompensationScript(timer);
        }

        if (result == null) {
            throw new IllegalStateException("Redis compensation script returned null");
        }

        TimeDealStockCompensationResult compensationResult = switch (Math.toIntExact(result)) {
            case TimeDealStockReservationLuaScript.COMPENSATION_COMPLETED ->
                    TimeDealStockCompensationResult.COMPENSATED;
            case TimeDealStockReservationLuaScript.COMPENSATION_ALREADY_COMPLETED ->
                    TimeDealStockCompensationResult.ALREADY_COMPENSATED;
            case TimeDealStockReservationLuaScript.COMPENSATION_STOCK_NOT_INITIALIZED ->
                    TimeDealStockCompensationResult.STOCK_NOT_INITIALIZED;
            default -> throw new IllegalStateException("Unknown Redis compensation result: " + result);
        };
        metrics.recordCompensationResult(compensationResult);
        return compensationResult;
    }
}
