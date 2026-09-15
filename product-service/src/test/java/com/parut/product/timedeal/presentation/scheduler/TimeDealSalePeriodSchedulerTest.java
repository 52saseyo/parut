package com.parut.product.timedeal.presentation.scheduler;

import com.parut.product.global.config.SchedulingConfig;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class TimeDealSalePeriodSchedulerTest {
    @Configuration
    @EnableScheduling
    static class SchedulingEnabled {
        @Bean
        TimeDealSalePeriodScheduler scheduler(TimeDealCommandUseCase useCase) {
            return new TimeDealSalePeriodScheduler(useCase);
        }
    }

    @Test
    void 타임딜_마감_작업이_끝나기_전에도_오픈_활성화_작업을_실행한다() throws Exception {
        CountDownLatch endStarted = new CountDownLatch(1);
        CountDownLatch releaseEnd = new CountDownLatch(1);
        CountDownLatch activated = new CountDownLatch(1);
        AtomicBoolean endFinished = new AtomicBoolean(false);
        AtomicBoolean activatedWhileEnding = new AtomicBoolean(false);
        TimeDealCommandUseCase useCase = mock(TimeDealCommandUseCase.class);
        doAnswer(invocation -> {
            endStarted.countDown();
            releaseEnd.await(10, TimeUnit.SECONDS);
            endFinished.set(true);
            return null;
        }).when(useCase).endTimeDeals();
        doAnswer(invocation -> {
            if (endStarted.await(5, TimeUnit.SECONDS)) {
                activatedWhileEnding.set(!endFinished.get());
                activated.countDown();
            }
            return null;
        }).when(useCase).activateTimeDeals();

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        try {
            context.registerBean(TimeDealCommandUseCase.class, () -> useCase);
            context.register(SchedulingConfig.class, SchedulingEnabled.class);
            context.refresh();
            assertThat(endStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(activated.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(activatedWhileEnding.get()).isTrue();
        } finally {
            releaseEnd.countDown();
            context.close();
        }
    }
}
