package com.parut.product.global.outbox.infrastructure.messaging;

import com.parut.product.global.outbox.application.port.out.OutboxMessagePublisher;
import com.parut.product.global.outbox.domain.OutboxEvent;
import com.parut.product.global.constant.KafkaTopicConstants;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxKafkaPublisher implements OutboxMessagePublisher {

    private static final String TRACE_ID_HEADER = "traceId";

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void publish(OutboxEvent event) {
        String topic = resolveTopic(event);
        ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, null, event.getPayload());
        record.headers().add(
                TRACE_ID_HEADER,
                event.getTraceId().getBytes(StandardCharsets.UTF_8)
        ); // NOTE: header에 traceId 를 넣어야하기에 기존 간단한버전보다 record를 별도 생성, header추가하는 방식

        // Kafka 전송 성공이 확인된 뒤 Publisher가 Outbox 상태를 변경할 수 있도록 join() 사용 동기 대기한다. 만약 join이없다면 비동기 방식이다.
        kafkaTemplate.send(record).join();
    }

    private String resolveTopic(OutboxEvent event) {
        if ("TIME_DEAL_OPENING_SOON".equals(event.getEventType())) {
            return KafkaTopicConstants.TIME_DEAL_OPENING_SOON;
        }
        throw new IllegalArgumentException(
                "지원하지 않는 Outbox 이벤트 타입입니다. eventType=" + event.getEventType()
        );
    }
}
