package com.parut.gatewayservice.infrastructure.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final String BLACKLIST_PREFIX = "LOGOUT:";
    private final ReactiveStringRedisTemplate redisTemplate;

    private static final List<String> OPEN_API_PATHS = List.of(
            "/api/v1/auth/login/user",   // 일반회원 로그인
            "/api/v1/auth/login/seller", // 판매자 로그인
            "/api/v1/auth/signup",       // 일반회원 회원가입
            "/api/v1/auth/login/admin",  // 관리자 로그인
            "/api/v1/sellers/apply"      // 판매자 입점 신청
    );


    // @Autowired 대신 생성자 주입 방식 사용 (권장)
    public JwtAuthFilter(ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            log.info("[Gateway 진입] 요청 경로: {}", path);

            // 1. Trace ID 추출 또는 생성 (어떤 요청이든 항상 발급/유지)
            String traceId = request.getHeaders().getFirst("X-Trace-Id");
            if (!StringUtils.hasText(traceId)) {
                traceId = UUID.randomUUID().toString();
            }

            final String finalTraceId = traceId; // 람다식 내부 사용을 위한 final 처리

            // 2. 오픈 API 경로 처리 (인증 생략, 단 Trace ID는 헤더에 추가하여 전달)
            if (isOpenApi(path)) {
                log.info("[Gateway 통과] 인증 생략 경로: {}", path);
                ServerHttpRequest mutatedRequest = request.mutate()
                        .headers(headers -> {
                            headers.remove("X-User-Id");
                            headers.remove("X-User-Role");
                            // headers.remove("X-Service-Key"); // 서비스 키도 사용한다면 제거
                            headers.set("X-Trace-Id", finalTraceId);
                        })
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

            // 3. Authorization 헤더 검증
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return handleUnauthorized(exchange, "유효하지 않은 토큰 형식입니다.");
            }

            String token = authHeader.substring(7);

            try {
                // 4. JWT 검증 및 데이터 추출
                Claims claims = validateToken(token);

                // 5. Redis 블랙리스트(로그아웃) 검증
                return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
                        .flatMap(isBlacklisted -> {
                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                return handleUnauthorized(exchange, "로그아웃 처리된 토큰입니다.");
                            }

                            // 6. JWT payload에서 User Info 추출
                            String userId = claims.getSubject();
                            String userRole = claims.get("role", String.class);

                            // 7. 다운스트림으로 전달할 3가지 필수 헤더 세팅
                            ServerHttpRequest mutatedRequest = request.mutate()
                                    .header("X-User-Id", userId)
                                    .header("X-User-Role", userRole)
                                    .header("X-Trace-Id", finalTraceId)
                                    .build();

                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        });

            } catch (JwtException e) {
                log.error("JWT 검증 실패: {}", e.getMessage());
                return handleUnauthorized(exchange, "만료되거나 조작된 토큰입니다.");
            }
        };
    }

    private boolean isOpenApi(String path) {
        return OPEN_API_PATHS.stream().anyMatch(path::startsWith);
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        log.warn("Unauthorized request: {}", message);
        return response.setComplete();
    }
}