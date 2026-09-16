package com.parut.order.global.config;

import com.parut.order.global.constant.HeaderConstants;
import com.parut.order.global.filter.TraceIdFilter;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;


public class InternalFeignConfig {

    @Bean
    public RequestInterceptor serviceKeyInterceptor(
            @Value("${internal.service-key}") String serviceKey
    ) {
        return template -> template.header(HeaderConstants.SERVICE_KEY, serviceKey);
    }

    /** 호출 체인이 같은 traceId 로 묶이도록 인입 요청의 값을 그대로 전파한다. */
    @Bean
    public RequestInterceptor traceIdInterceptor() {
        return template -> {
            String traceId = TraceIdFilter.currentTraceId();

            // 스케줄러, 비동기 스레드에서 호출하면 MDC 가 비어 있어 있음.
            if (StringUtils.hasText(traceId)) {
                template.header(HeaderConstants.TRACE_ID, traceId);
            }
        };
    }
}
