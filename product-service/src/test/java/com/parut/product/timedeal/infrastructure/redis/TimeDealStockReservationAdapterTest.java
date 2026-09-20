package com.parut.product.timedeal.infrastructure.redis;

import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockReservationResult;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockCompensationResult;
import com.parut.product.timedeal.application.metrics.timedeal.TimeDealRedisMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeDealStockReservationAdapterTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RScript script;

    @Mock
    private TimeDealRedisMetrics metrics;

    private TimeDealStockReservationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TimeDealStockReservationAdapter(redissonClient, metrics);
        when(redissonClient.getScript()).thenReturn(script);
    }

    @Test
    void Redis_성공코드를_RESERVED로_변환한다() {
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.LONG),
                anyList(),
                any(Object[].class)
        )).thenReturn(1L);

        TimeDealStockReservationResult result = adapter.reserve(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);

        assertThat(result).isEqualTo(TimeDealStockReservationResult.RESERVED);
        verify(script).eval(
                eq(RScript.Mode.READ_WRITE),
                eq(TimeDealStockReservationLuaScript.SCRIPT),
                eq(RScript.ReturnType.LONG),
                anyList(),
                eq(1),
                eq(900L)
        );
    }

    @Test
    void Redis_품절코드를_SOLD_OUT으로_변환한다() {
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.LONG),
                anyList(),
                any(Object[].class)
        )).thenReturn(0L);

        TimeDealStockReservationResult result = adapter.reserve(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);

        assertThat(result).isEqualTo(TimeDealStockReservationResult.SOLD_OUT);
    }

    @Test
    void Redis_중복코드를_DUPLICATE_ORDER로_변환한다() {
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.LONG),
                anyList(),
                any(Object[].class)
        )).thenReturn(2L);

        TimeDealStockReservationResult result = adapter.reserve(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);

        assertThat(result).isEqualTo(TimeDealStockReservationResult.DUPLICATE_ORDER);
    }

    @Test
    void Redis_보상완료코드를_COMPENSATED로_변환한다() {
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.LONG),
                anyList(),
                any(Object[].class)
        )).thenReturn(1L);

        TimeDealStockCompensationResult result = adapter.compensate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isEqualTo(TimeDealStockCompensationResult.COMPENSATED);
        verify(script).eval(
                eq(RScript.Mode.READ_WRITE),
                eq(TimeDealStockReservationLuaScript.COMPENSATION_SCRIPT),
                eq(RScript.ReturnType.LONG),
                anyList()
        );
    }
}
