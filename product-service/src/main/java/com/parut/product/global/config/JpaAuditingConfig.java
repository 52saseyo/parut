package com.parut.product.global.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Auditing(@CreatedBy, @LastModifiedBy) 활성화 및 "현재 요청자가 누구인지"를
 * Spring에게 알려주는 AuditorAware 구현체를 등록하는 설정 클래스.
 *
 * Gateway가 인증을 선행 처리하고, 하위 서비스(Product)에는 X-User-Id 헤더로
 * 사용자 ID를 전달하는 구조를 전제로 한다. 이 클래스는 그 헤더 값을 꺼내서
 * BaseEntity/BaseUpdatableEntity의 createdBy, updatedBy 필드에 자동으로 채워준다.
 */
@Configuration
@EnableJpaAuditing // 이 설정이 있어야 @CreatedDate/@CreatedBy 등이 실제로 자동 동작함

public class JpaAuditingConfig {
    /**
     * "현재 작업을 수행하는 사용자가 누구인지" Spring Data JPA에게 알려주는 빈.
     * @CreatedBy, @LastModifiedBy가 붙은 필드는 엔티티 저장/수정 시점에
     * 이 메서드가 반환하는 값으로 자동 채워진다.
     */
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            // 현재 스레드에 바인딩된 HTTP 요청 정보를 가져옴
            // (Spring MVC가 요청 처리 스레드마다 이 정보를 저장해둠)
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                // HTTP 요청 컨텍스트 자체가 없는 경우
                // 예: 재고 만료 처리 스케줄러, 배치, @Async로 실행되는 별도 스레드 등
                // → 이런 경우 "요청자"라는 개념이 없으므로 빈 값 반환
                return Optional.empty();
            }

            HttpServletRequest request = attributes.getRequest();
            // Gateway가 인증 완료 후 붙여주는 헤더에서 사용자 ID를 꺼냄
            String userIdHeader = request.getHeader(HeaderConstants.USER_ID);

            if (userIdHeader == null || userIdHeader.isBlank()) {
                // 헤더 자체가 없는 요청 (예: 인증이 필요 없는 공개 API 등)
                return Optional.empty();
            }

            try {
                // 헤더 값을 UUID로 변환해서 반환
                return Optional.of(UUID.fromString(userIdHeader));
            } catch (IllegalArgumentException e) {
                // 헤더 값이 있지만 UUID 형식이 아닌 비정상적인 경우
                // → 예외를 그대로 던지지 않고 안전하게 빈 값으로 처리
                return Optional.empty();
            }
        };
    }

}

