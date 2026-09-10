package com.parut.product.global.config;

import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.global.interceptor.ServiceKeyInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    private static final String SYSTEM_USER_ID = "00000000-0000-0000-0000-000000000001";

    @Bean
    public AuditorAware<String> auditorProvider() {

        return () -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.empty();
            }

            HttpServletRequest request = attributes.getRequest();

            // ServiceKeyInterceptor를 통과한 내부 서비스 요청
            Object internalRequest = request.getAttribute(ServiceKeyInterceptor.INTERNAL_REQUEST_ATTRIBUTE);

            if (Boolean.TRUE.equals(internalRequest)) {
                return Optional.of(SYSTEM_USER_ID);
            }

            // 일반 사용자 요청USER_ID
            String userIdHeader = request.getHeader(HeaderConstants.USER_ID);

            if (userIdHeader == null || userIdHeader.isBlank()) {
                throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
            }

            try {
                return Optional.of(userIdHeader);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        };
    }
}

