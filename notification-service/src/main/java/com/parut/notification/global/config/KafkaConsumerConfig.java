package com.parut.notification.global.config;

import java.util.Map;

import com.parut.notification.global.constant.KafkaTopicConstants;
import com.parut.notification.global.exception.InvalidKafkaEventException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {
    private static final long RETRY_INTERVAL_MILLIS = 2_000L;
    private static final long RETRY_COUNT = 3L;
    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, String> kafkaConsumerFactory() {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties();

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                        new TopicPartition(
                                KafkaTopicConstants.TIME_DEAL_OPENING_SOON_DLT,
                                record.partition()
                        )
        );

        /**
         * DLT 전송에 실패했을 때 해당 메시지를 정상적으로 복구했다고 처리하지 않는다.
         */
        recoverer.setFailIfSendResultIsError(true);

        return recoverer;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {

        /**
         * 최초 실행 후 2초 간격으로 최대 3번 재시도
         */
        FixedBackOff backOff = new FixedBackOff(RETRY_INTERVAL_MILLIS, RETRY_COUNT);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        /**
         * 메시지 자체가 잘못된 경우에는 재시도 없이 바로 DLT로 보낸다.
         */
        errorHandler.addNotRetryableExceptions(InvalidKafkaEventException.class);


        return errorHandler;
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory(JsonMapper jsonMapper, DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(kafkaConsumerFactory());

        // JSON 문자열을 Listener 파라미터의 이벤트 DTO로 변환
        factory.setRecordMessageConverter(new StringJacksonJsonMessageConverter(jsonMapper));

        /**
         * Listener에서 예외 발생 시 재시도와 DLT 처리
         */
        factory.setCommonErrorHandler(errorHandler);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

}
