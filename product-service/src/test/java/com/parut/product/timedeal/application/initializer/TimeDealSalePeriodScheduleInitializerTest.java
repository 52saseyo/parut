package com.parut.product.timedeal.application.initializer;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealScheduleEntry;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealScheduleRedisPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimeDealSalePeriodScheduleInitializerTest {

    @Mock
    private TimeDealRepository timeDealRepository;

    @Mock
    private TimeDealScheduleRedisPort scheduleRedisPort;

    @Test
    void 앱_시작_시_진행_가능한_타임딜의_판매_기간을_Redis에_복구한다() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        Instant firstStartAt = Instant.parse("2026-09-28T01:00:00Z");
        Instant firstEndAt = Instant.parse("2026-09-28T02:00:00Z");
        Instant secondStartAt = Instant.parse("2026-09-29T01:00:00Z");
        Instant secondEndAt = Instant.parse("2026-09-29T02:00:00Z");

        when(timeDealRepository.findSalePeriodSchedulesAfter(null, 500)).thenReturn(List.of(
                new TimeDealScheduleEntry(firstId, firstStartAt, firstEndAt),
                new TimeDealScheduleEntry(secondId, secondStartAt, secondEndAt)
        ));

        TimeDealSalePeriodScheduleInitializer initializer =
                new TimeDealSalePeriodScheduleInitializer(timeDealRepository, scheduleRedisPort);

        initializer.reconcile();

        InOrder inOrder = inOrder(scheduleRedisPort);
        inOrder.verify(scheduleRedisPort).schedule(firstId, firstStartAt, firstEndAt);
        inOrder.verify(scheduleRedisPort).schedule(secondId, secondStartAt, secondEndAt);
        verify(timeDealRepository).findSalePeriodSchedulesAfter(null, 500);
    }
}
