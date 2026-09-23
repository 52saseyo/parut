package com.parut.product.global.outbox.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-22T10:00:00Z");
    private static final Instant PUBLISHED_AT = Instant.parse("2026-09-22T10:00:05Z");

    @Test
    void pending_상태로_생성된다() {
        OutboxEvent event = pending();

        assertThat(event.getPublishStatus()).isEqualTo(OutboxPublishStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void 발행에_성공하면_published로_변경된다() {
        OutboxEvent event = pending();

        event.markPublished(PUBLISHED_AT);

        assertThat(event.getPublishStatus()).isEqualTo(OutboxPublishStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void 발행_실패는_pending과_재시도_정보로_기록된다() {
        OutboxEvent event = pending();

        event.markRetryableFailure("Kafka unavailable");

        assertThat(event.getPublishStatus()).isEqualTo(OutboxPublishStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("Kafka unavailable");
    }

    @Test
    void 재시도_한도_초과는_failed로_변경된다() {
        OutboxEvent event = pending();

        event.markFailed("retry limit exceeded");

        assertThat(event.getPublishStatus()).isEqualTo(OutboxPublishStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
    }

    @Test
    void 필수값이_없으면_생성할_수_없다() {
        assertThatThrownBy(() -> OutboxEvent.pending(
                UUID.randomUUID(),
                "TIME_DEAL_OPENING_SOON",
                UUID.randomUUID(),
                "2026-09-22T10:10:00Z",
                "trace-id",
                "{}",
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private static OutboxEvent pending() {
        return OutboxEvent.pending(
                UUID.randomUUID(),
                "TIME_DEAL_OPENING_SOON",
                UUID.randomUUID(),
                "2026-09-22T10:10:00Z",
                "trace-id",
                "{\"eventId\":\"event\"}",
                CREATED_AT
        );
    }
}
