package com.parut.product.timedeal.application.metrics.timedeal;

import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockCompensationResult;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockReservationResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDealRedisMetricsTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final TimeDealRedisMetrics metrics = new TimeDealRedisMetrics(meterRegistry);

    @Test
    void Redis_선점과_보상_결과를_카운터로_기록한다() {
        metrics.recordReservationResult(TimeDealStockReservationResult.RESERVED);
        metrics.recordReservationResult(TimeDealStockReservationResult.SOLD_OUT);
        metrics.recordCompensationResult(TimeDealStockCompensationResult.COMPENSATED);

        assertThat(meterRegistry.get("timedeal.redis.reservation")
                .tag("result", "RESERVED").counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("timedeal.redis.reservation")
                .tag("result", "SOLD_OUT").counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("timedeal.redis.compensation")
                .tag("result", "COMPENSATED").counter().count()).isEqualTo(1);
    }

    @Test
    void Redis_스크립트_실행시간을_타이머로_기록한다() {
        Timer.Sample sample = metrics.startReservationScript();
        metrics.stopReservationScript(sample);

        assertThat(meterRegistry.get("timedeal.redis.reservation.time")
                .timer().count()).isEqualTo(1);
    }
}
