package com.parut.product.global.config;

import com.parut.product.global.constant.HeaderConstants;
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
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.empty();
            }

            HttpServletRequest request = attributes.getRequest();
            String userIdHeader = request.getHeader(HeaderConstants.USER_ID);

            if (userIdHeader == null || userIdHeader.isBlank()) {
                return Optional.empty();
            }

            try {
                return Optional.of(userIdHeader);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        };
    }
}

