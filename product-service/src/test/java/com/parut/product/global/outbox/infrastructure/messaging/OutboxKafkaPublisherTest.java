package com.parut.product.global.outbox.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.global.constant.KafkaTopicConstants;
import com.parut.product.global.outbox.domain.OutboxEvent;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class OutboxKafkaPublisherTest {

    private final KafkaTemplate<String, String> kafkaTemplate =
            org.mockito.Mockito.mock(KafkaTemplate.class);
    private final OutboxKafkaPublisher publisher =
            new OutboxKafkaPublisher(kafkaTemplate);

    @BeforeEach
    void setUp() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void 시작임박_이벤트를_키없이_발행하고_traceId를_헤더로_전달한다() {
        String traceId = "trace-123";
        OutboxEvent event = OutboxEvent.pending(
                UUID.randomUUID(),
                "TIME_DEAL_OPENING_SOON",
                UUID.randomUUID(),
                Instant.parse("2026-09-23T10:10:00Z").toString(),
                traceId,
                "{\"timeDealId\":\"deal\"}",
                Instant.parse("2026-09-23T10:00:00Z")
        );

        publisher.publish(event);

        org.mockito.ArgumentCaptor<ProducerRecord<String, String>> captor =
                org.mockito.ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, String> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(KafkaTopicConstants.TIME_DEAL_OPENING_SOON);
        assertThat(record.key()).isNull();
        assertThat(record.value()).isEqualTo("{\"timeDealId\":\"deal\"}");
        assertThat(record.headers().lastHeader(HeaderConstants.TRACE_ID).value())
                .isEqualTo(traceId.getBytes(StandardCharsets.UTF_8));
    }
}
