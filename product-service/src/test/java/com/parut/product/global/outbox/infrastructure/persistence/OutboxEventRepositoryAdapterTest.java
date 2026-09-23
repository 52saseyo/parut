package com.parut.product.global.outbox.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.parut.product.global.outbox.domain.OutboxEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventRepositoryAdapterTest {

    private final JpaOutboxEventRepository jpaRepository =
            org.mockito.Mockito.mock(JpaOutboxEventRepository.class);
    private final OutboxEventRepositoryAdapter adapter = new OutboxEventRepositoryAdapter(jpaRepository);

    @Test
    void 신규_이벤트가_저장되면_true를_반환한다() {
        OutboxEvent event = pending();
        when(jpaRepository.insertIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(event.getEventId()),
                org.mockito.ArgumentMatchers.eq(event.getEventType()),
                org.mockito.ArgumentMatchers.eq(event.getAggregateId()),
                org.mockito.ArgumentMatchers.eq(event.getDeduplicationKey()),
                org.mockito.ArgumentMatchers.eq(event.getTraceId()),
                org.mockito.ArgumentMatchers.eq(event.getPayload()),
                org.mockito.ArgumentMatchers.eq("PENDING"),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(event.getCreatedAt())
        )).thenReturn(1);

        assertThat(adapter.saveIfAbsent(event)).isTrue();
    }

    @Test
    void 동일한_멱등키가_있으면_false를_반환한다() {
        OutboxEvent event = pending();
        when(jpaRepository.insertIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(0);

        assertThat(adapter.saveIfAbsent(event)).isFalse();
    }

    private static OutboxEvent pending() {
        return OutboxEvent.pending(
                UUID.randomUUID(),
                "TIME_DEAL_OPENING_SOON",
                UUID.randomUUID(),
                "2026-09-22T10:10:00Z",
                "trace-id",
                "{\"eventId\":\"event\"}",
                Instant.parse("2026-09-22T10:00:00Z")
        );
    }
}
