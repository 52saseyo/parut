// Auth 도메인의 RefreshToken 엔티티 (Redis 또는 DB 저장용)
package com.parut.user.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefreshToken {
    private String username;
    private String refreshToken;
}