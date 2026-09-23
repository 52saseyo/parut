package com.parut.product.global.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parut.product.global.outbox.application.port.out.OutboxEventRepository;
import com.parut.product.global.outbox.domain.OutboxEvent;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutboxPublisherTest {

    private final OutboxEventRepository outboxEventRepository =
            org.mockito.Mockito.mock(OutboxEventRepository.class);
    private final OutboxPublisher outboxPublisher =
            new OutboxPublisher(outboxEventRepository);

    @Test
    void PENDING_이벤트를_배치크기와_함께_조회한다() {
        List<OutboxEvent> pendingEvents = List.of();
        when(outboxEventRepository.findPending(50)).thenReturn(pendingEvents);

        List<OutboxEvent> result = outboxPublisher.loadPendingEvents(50);

        assertThat(result).isSameAs(pendingEvents);
        verify(outboxEventRepository).findPending(50);
    }

    @Test
    void 잘못된_배치크기는_Repository에서_검증한다() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("limit은 1 이상 100 이하여야 합니다."))
                .when(outboxEventRepository).findPending(0);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> outboxPublisher.loadPendingEvents(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
