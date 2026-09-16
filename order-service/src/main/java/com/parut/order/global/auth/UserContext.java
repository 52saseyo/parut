package com.parut.order.global.auth;

import java.util.UUID;

/**
 * 인증 헤더에서 확인한 요청자 정보. {@code UserContextInterceptor} 가 검증 후 요청 속성에 담고,
 * ArgumentResolver 가 컨트롤러 파라미터로 주입한다.
 */
public record UserContext(
        UUID userId,
        UserRole role
) {

    public static UserContext of(
            UUID userId,
            UserRole role
    ) {
        return new UserContext(userId, role);
    }
}
