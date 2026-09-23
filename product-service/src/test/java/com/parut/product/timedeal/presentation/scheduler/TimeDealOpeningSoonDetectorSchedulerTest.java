package com.parut.product.timedeal.presentation.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.parut.product.timedeal.application.command.timedeal.TimeDealOpeningSoonDetector;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TimeDealOpeningSoonDetectorSchedulerTest {

    @Test
    void 시작_임박_감지기를_현재_시각으로_호출한다() {
        TimeDealOpeningSoonDetector detector =
                org.mockito.Mockito.mock(TimeDealOpeningSoonDetector.class);
        TimeDealOpeningSoonDetectorScheduler scheduler =
                new TimeDealOpeningSoonDetectorScheduler(detector);

        scheduler.detectOpeningSoonTimeDeals();

        verify(detector).detect(any(Instant.class));
    }
}
