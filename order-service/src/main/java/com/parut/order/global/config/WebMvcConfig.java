package com.parut.order.global.config;

import com.parut.order.global.interceptor.ServiceKeyInterceptor;
import com.parut.order.global.interceptor.UserContextInterceptor;
import com.parut.order.global.resolver.UserContextArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;


@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /** 서비스 간 내부 호출 경로. 이 아래만 공유 시크릿 검증 대상이다. */
    private static final String INTERNAL_API_PATTERN = "/api/v1/internal/**";

    /** 클라이언트 인입 경로 전체. 새 컨트롤러가 인증 없이 열리지 않도록 기본 적용 대상으로 둔다. */
    private static final String CLIENT_API_PATTERN = "/api/v1/**";

    private final ServiceKeyInterceptor serviceKeyInterceptor;
    private final UserContextInterceptor userContextInterceptor;
    private final UserContextArgumentResolver userContextArgumentResolver;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(serviceKeyInterceptor)
                .addPathPatterns(INTERNAL_API_PATTERN);

        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns(CLIENT_API_PATTERN)
                .excludePathPatterns(INTERNAL_API_PATTERN);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userContextArgumentResolver);
    }
}
