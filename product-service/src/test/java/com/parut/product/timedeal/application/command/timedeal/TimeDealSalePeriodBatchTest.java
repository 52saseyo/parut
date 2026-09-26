package com.parut.product.timedeal.application.command.timedeal;

import com.parut.product.global.common.AuditorContext;
import com.parut.product.global.constant.AuditorConstants;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealScheduleRedisPort;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TimeDealSalePeriodBatchTest {
    @Test
    void 오픈_처리_실패건은_남기고_성공건은_스케줄에서_제거한다() {
        TimeDealRepository repository = mock(TimeDealRepository.class);
        TimeDealScheduleRedisPort scheduleRedisPort = mock(TimeDealScheduleRedisPort.class);
        TimeDealSalePeriodProcessor processor = mock(TimeDealSalePeriodProcessor.class);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        when(scheduleRedisPort.findOpenDue(any(), eq(100))).thenReturn(List.of(first, second, third));
        doThrow(new IllegalStateException("실패 건은 다음 실행에 재시도")).when(processor).synchronize(first);
        doAnswer(invocation -> {
            assertThat(AuditorContext.get()).contains(AuditorConstants.BATCH_SYSTEM_USER_ID);
            return null;
        }).when(processor).synchronize(second);

        new TimeDealCommandService(processor, repository, scheduleRedisPort,
                null, null, null, null, null, null).activateTimeDeals();

        verify(processor).synchronize(first);
        verify(scheduleRedisPort).removeOpen(second);
        verify(scheduleRedisPort).removeOpen(third);
        verify(scheduleRedisPort, never()).removeOpen(first);
        verify(processor).synchronize(second);
        verify(processor).synchronize(third);
        assertThat(AuditorContext.get()).isEmpty();
    }

    @Test
    void 마감_처리_실패건은_남기고_성공건은_스케줄에서_제거한다() {
        TimeDealRepository repository = mock(TimeDealRepository.class);
        TimeDealScheduleRedisPort scheduleRedisPort = mock(TimeDealScheduleRedisPort.class);
        TimeDealSalePeriodProcessor processor = mock(TimeDealSalePeriodProcessor.class);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        when(scheduleRedisPort.findCloseDue(any(), eq(100)))
                .thenReturn(List.of(first, second, third));
        doThrow(new IllegalStateException("실패 건은 다음 실행에 재시도"))
                .when(processor).synchronize(first);
        doAnswer(invocation -> {
            assertThat(AuditorContext.get())
                    .contains(AuditorConstants.BATCH_SYSTEM_USER_ID);
            return null;
        }).when(processor).synchronize(second);

        new TimeDealCommandService(processor, repository, scheduleRedisPort,
                null, null, null, null, null, null)
                .endTimeDeals();

        verify(processor).synchronize(first);
        verify(processor).synchronize(second);
        verify(processor).synchronize(third);
        verify(scheduleRedisPort).removeClose(second);
        verify(scheduleRedisPort).removeClose(third);
        verify(scheduleRedisPort, never()).removeClose(first);
        assertThat(AuditorContext.get()).isEmpty();
    }
}
