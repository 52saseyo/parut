package com.parut.order.global.config;

import com.parut.order.global.constant.HeaderConstants;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;


public class InternalFeignConfig {

    @Bean
    public RequestInterceptor serviceKeyInterceptor(
            @Value("${internal.service-key}") String serviceKey
    ) {
        return template -> template.header(HeaderConstants.SERVICE_KEY, serviceKey);
    }
}
