package com.parut.product.global.config;

import com.parut.product.global.interceptor.ServiceKeyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /** 서비스 간 내부 호출 경로. 이 아래만 공유 시크릿 검증 대상이다. */
    private static final String INTERNAL_API_PATTERN = "/api/v1/internal/**";

    private final ServiceKeyInterceptor serviceKeyInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(serviceKeyInterceptor)
                .addPathPatterns(INTERNAL_API_PATTERN);
    }
}