package com.parut.gatewayservice.infrastructure.filter;

import com.parut.gatewayservice.dto.UserVerifyResponse;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
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

    @Value("${USER_URL:http://user-service:8080}")
    private String userServiceUrl;

    private static final String BLACKLIST_PREFIX = "LOGOUT:";
    private final ReactiveStringRedisTemplate redisTemplate;

    private final WebClient webClient;

    private static final List<String> OPEN_API_PATHS = List.of(
            "/api/v1/auth/login/user",   // 일반회원 로그인
            "/api/v1/auth/login/seller", // 판매자 로그인
            "/api/v1/auth/signup",       // 일반회원 회원가입
            "/api/v1/auth/login/admin",  // 관리자 로그인
            "/api/v1/sellers/apply"      // 판매자 입점 신청
    );

    /** 상품·타임딜 목록과 1단계 상세 조회는 비로그인 공개 API로 제공한다. */
    private static final List<String> PUBLIC_READ_PATHS = List.of(
            "/api/v1/products",
            "/api/v1/time-deals"
    );


    // @Autowired 대신 생성자 주입 방식 사용 (권장)
    public JwtAuthFilter(WebClient.Builder webClientBuilder, ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
        this.webClient = webClientBuilder.build();
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

            // CORS preflight는 인증 대상이 아니다. CorsWebFilter가 응답을 처리하지 못한 경우에도
            // JWT 필터가 OPTIONS 요청을 401로 막지 않도록 먼저 통과시킨다.
            if (isCorsPreflight(request)) {
                return chain.filter(exchange);
            }

            // 상품·타임딜 목록/상세 조회는 각 Controller 정책에 맞춰 비로그인 공개 API로 통과시킨다.
            // 상품·타임딜 등록/수정/삭제, 재고·구매 API는 기존처럼 JWT 인증을 거친다.
            if (isPublicRead(request)) {
                ServerHttpRequest mutatedRequest = request.mutate()
                        .headers(headers -> {
                            headers.remove("X-User-Id");
                            headers.remove("X-User-Role");
                            headers.set("X-Trace-Id", finalTraceId);
                        })
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

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

                            // 6. JWT payload에서 '임시 식별자'만 추출
                            String jwtUserId = claims.getSubject();
                            String role = claims.get("role", String.class);

                            // 7. [핵심 로직 추가] user-service로 신뢰할 수 있는 데이터 조회
                            return webClient.get()
                                    .uri(userServiceUrl + "/api/v1/internal/users/" + jwtUserId + "/verify?role=" + role)
                                    .retrieve()
                                    .bodyToMono(UserVerifyResponse.class) // 앞서 만든 DTO 클래스
                                    .flatMap(verifyResult -> {
                                        // 8. DB 검증 결과, 유효하지 않은 유저(탈퇴, 정지 등) 차단
                                        if (!verifyResult.isValid()) {
                                            log.warn("[Gateway 차단] DB 검증 실패 유저: {}", jwtUserId);
                                            return handleUnauthorized(exchange, "존재하지 않거나 정지된 사용자입니다.");
                                        }

                                        // 9. 검증 완료: JWT 내용이 아닌 'DB에서 갓 꺼낸 데이터'로 헤더 덮어쓰기
                                        ServerHttpRequest mutatedRequest = request.mutate()
                                                .headers(headers -> {
                                                    headers.remove("X-User-Id");
                                                    headers.remove("X-User-Role");
                                                    headers.set("X-User-Id", verifyResult.userId());
                                                    headers.set("X-User-Role", verifyResult.role());
                                                    headers.set("X-Trace-Id", finalTraceId);
                                                })
                                                .build();

                                        log.info("[Gateway 통과] 사용자 검증 완료: {}", verifyResult.userId());
                                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                                    })
                                    // WebClient 통신 중 에러 발생 시 예외 처리
                                    .onErrorResume(e -> {
                                        log.error("[Gateway 에러] user-service 통신 실패: {}", e.getMessage());
                                        return handleUnauthorized(exchange, "인증 서버 통신에 실패했습니다.");
                                    });
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

    private boolean isCorsPreflight(ServerHttpRequest request) {
        return request.getMethod() == HttpMethod.OPTIONS
                && StringUtils.hasText(request.getHeaders().getFirst(HttpHeaders.ORIGIN))
                && StringUtils.hasText(request.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD));
    }

    private boolean isPublicRead(ServerHttpRequest request) {
        if (request.getMethod() != HttpMethod.GET) {
            return false;
        }

        String path = request.getPath().value();
        if (path.equals("/api/v1/time-deals/seller")
                || path.startsWith("/api/v1/time-deals/seller/")) {
            return false;
        }

        return PUBLIC_READ_PATHS.stream().anyMatch(basePath -> isCollectionOrDetailPath(path, basePath));
    }

    private boolean isCollectionOrDetailPath(String path, String basePath) {
        if (path.equals(basePath)) {
            return true;
        }

        String remainder = path.substring(Math.min(path.length(), basePath.length() + 1));
        return path.startsWith(basePath + "/") && !remainder.contains("/");
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
