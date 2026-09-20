package com.parut.product.timedeal.application.command.timedeal;

import com.parut.product.global.common.AuditorContext;
import com.parut.product.global.constant.AuditorConstants;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TimeDealSalePeriodBatchTest {
    @Test
    void 실패해도_다음건을_처리하고_커서로_다음페이지를_읽는다() {
        TimeDealRepository repository = mock(TimeDealRepository.class);
        TimeDealSalePeriodProcessor processor = mock(TimeDealSalePeriodProcessor.class);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        when(repository.findTimeDealsToActivate(any(), isNull(), eq(100))).thenReturn(List.of(first, second));
        when(repository.findTimeDealsToActivate(any(), eq(second), eq(100))).thenReturn(List.of(third));
        when(repository.findTimeDealsToActivate(any(), eq(third), eq(100))).thenReturn(List.of());
        doThrow(new IllegalStateException("실패 건은 다음 실행에 재시도")).when(processor).synchronize(first);
        doAnswer(invocation -> {
            assertThat(AuditorContext.get()).contains(AuditorConstants.BATCH_SYSTEM_USER_ID);
            return null;
        }).when(processor).synchronize(second);

        new TimeDealCommandService(processor, repository, null, null, null, null, null, null).activateTimeDeals();

        verify(processor).synchronize(first);
        verify(repository, never()).findTimeDealsToEnd(any(), any(), anyInt());
        verify(processor).synchronize(second);
        verify(processor).synchronize(third);
        assertThat(AuditorContext.get()).isEmpty();
    }

    @Test
    void 마감_처리도_실패한_건을_건너뛰고_커서로_다음페이지를_읽는다() {
        TimeDealRepository repository = mock(TimeDealRepository.class);
        TimeDealSalePeriodProcessor processor = mock(TimeDealSalePeriodProcessor.class);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        when(repository.findTimeDealsToEnd(any(), isNull(), eq(100)))
                .thenReturn(List.of(first, second));
        when(repository.findTimeDealsToEnd(any(), eq(second), eq(100)))
                .thenReturn(List.of(third));
        when(repository.findTimeDealsToEnd(any(), eq(third), eq(100)))
                .thenReturn(List.of());
        doThrow(new IllegalStateException("실패 건은 다음 실행에 재시도"))
                .when(processor).synchronize(first);
        doAnswer(invocation -> {
            assertThat(AuditorContext.get())
                    .contains(AuditorConstants.BATCH_SYSTEM_USER_ID);
            return null;
        }).when(processor).synchronize(second);

        new TimeDealCommandService(processor, repository, null, null, null, null, null, null)
                .endTimeDeals();

        verify(processor).synchronize(first);
        verify(processor).synchronize(second);
        verify(processor).synchronize(third);
        verify(repository).findTimeDealsToEnd(any(), isNull(), eq(100));
        verify(repository).findTimeDealsToEnd(any(), eq(second), eq(100));
        verify(repository).findTimeDealsToEnd(any(), eq(third), eq(100));
        verify(repository, never()).findTimeDealsToActivate(any(), any(), anyInt());
        assertThat(AuditorContext.get()).isEmpty();
    }
}
