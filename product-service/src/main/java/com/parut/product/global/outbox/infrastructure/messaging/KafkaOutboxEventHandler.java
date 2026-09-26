package com.parut.product.global.outbox.infrastructure.messaging;

import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.global.constant.KafkaTopicConstants;
import com.parut.product.global.outbox.application.port.out.OutboxEventHandler;
import com.parut.product.global.outbox.domain.OutboxEvent;
import com.parut.product.timedeal.application.event.timedeal.TimeDealOpeningSoonEvent;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaOutboxEventHandler implements OutboxEventHandler {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public boolean supports(String eventType) {
        return TimeDealOpeningSoonEvent.EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        String topic = resolveTopic(event.getEventType());
        ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, null, event.getPayload());
        record.headers().add(
                HeaderConstants.TRACE_ID,
                event.getTraceId().getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record).join();
    }

    private String resolveTopic(String eventType) {
        if (TimeDealOpeningSoonEvent.EVENT_TYPE.equals(eventType)) {
            return KafkaTopicConstants.TIME_DEAL_OPENING_SOON;
        }
        throw new IllegalArgumentException("지원하지 않는 Kafka eventType입니다. eventType=" + eventType);
    }
}
