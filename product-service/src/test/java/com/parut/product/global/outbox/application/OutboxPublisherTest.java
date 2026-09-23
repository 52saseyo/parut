package com.parut.product.global.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parut.product.global.outbox.application.port.out.OutboxEventRepository;
import com.parut.product.global.outbox.application.port.out.OutboxMessagePublisher;
import com.parut.product.global.outbox.domain.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxPublisherTest {

    private final OutboxEventRepository outboxEventRepository =
            org.mockito.Mockito.mock(OutboxEventRepository.class);
    private final OutboxMessagePublisher outboxMessagePublisher =
            org.mockito.Mockito.mock(OutboxMessagePublisher.class);
    private final OutboxPublisher outboxPublisher =
            new OutboxPublisher(outboxEventRepository, outboxMessagePublisher);

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

    @Test
    void Kafka_발행에_성공하면_PUBLISHED로_저장한다() {
        OutboxEvent event = pending();
        when(outboxEventRepository.findPending(50)).thenReturn(List.of(event));

        int publishedCount = outboxPublisher.publishPendingEvents(50);

        assertThat(publishedCount).isOne();
        assertThat(event.getPublishStatus().name()).isEqualTo("PUBLISHED");
        verify(outboxMessagePublisher).publish(event);
        verify(outboxEventRepository).save(event);
    }

    @Test
    void Kafka_발행에_실패하면_PUBLISHED로_변경하지않고_재시도한다() {
        OutboxEvent event = pending();
        when(outboxEventRepository.findPending(50)).thenReturn(List.of(event));
        org.mockito.Mockito.doThrow(new IllegalStateException("Kafka unavailable"))
                .when(outboxMessagePublisher).publish(event);

        int publishedCount = outboxPublisher.publishPendingEvents(50);

        assertThat(publishedCount).isZero();
        assertThat(event.getPublishStatus().name()).isEqualTo("PENDING");
        assertThat(event.getRetryCount()).isOne();
        assertThat(event.getLastError()).isEqualTo("Kafka unavailable");
        verify(outboxEventRepository).save(event);
    }

    private static OutboxEvent pending() {
        return OutboxEvent.pending(
                UUID.randomUUID(),
                "TIME_DEAL_OPENING_SOON",
                UUID.randomUUID(),
                Instant.parse("2026-09-23T10:10:00Z").toString(),
                "trace-id",
                "{\"eventId\":\"event\"}",
                Instant.parse("2026-09-23T10:00:00Z")
        );
    }
}
