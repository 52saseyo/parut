package com.parut.notification.global.config;

import com.parut.notification.global.constant.KafkaTopicConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic timeDealOpeningSoonDltTopic(){
        return TopicBuilder
                .name(
                        KafkaTopicConstants.TIME_DEAL_OPENING_SOON_DLT
                )
                .partitions(3)
                .replicas(1)
                .build();
    }
}
