package com.parut.user.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpireMillis;
    private final long refreshTokenExpireMillis;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expire}") Duration accessTokenExpire,
            @Value("${jwt.refresh-token-expire}") Duration refreshTokenExpire
    ) {
        // 0.12.x 버전부터는 SecretKey 타입으로 직접 관리하는 것이 좋습니다.
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpireMillis = accessTokenExpire.toMillis();
        this.refreshTokenExpireMillis = refreshTokenExpire.toMillis();
    }

    // Access Token 생성
    public String createAccessToken(UUID userId, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenExpireMillis);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(UUID userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenExpireMillis);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    // 토큰에서 User ID 추출
    public UUID getUserId(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    // 토큰에서 Role 추출
    public String getRole(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        /*try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }*/
        try {
            parseClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("지원되지 않는 형식이거나 손상된 JWT 토큰입니다. (Bearer 접두사 확인 필요)");
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    // 남은 유효시간 계산 (로그아웃 블랙리스트 등록용)
    public long getExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        long now = new Date().getTime();
        return (expiration.getTime() - now);
    }

    // ★ 0.12.x 버전 전용 파싱 로직
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}