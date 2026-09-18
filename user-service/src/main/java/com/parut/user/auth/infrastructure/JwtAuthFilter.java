package com.parut.user.auth.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String header = request.getHeader("Authorization");
//
//        if (header != null && header.startsWith("Bearer ")) {
//            String token = header.substring(7);
//
//            if (jwtProvider.validateToken(token)) {
//                // Redis에 로그아웃된 토큰인지 확인 (Blacklist Check)
//                String isLogout = redisTemplate.opsForValue().get("LOGOUT:" + token);
//                if (isLogout == null) {
//                    UUID userId = jwtProvider.getUserId(token);
//                    String role = jwtProvider.getRole(token);
//
//                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                            userId, null, List.of(new SimpleGrantedAuthority(role))
//                    );
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//                }
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 게이트웨이에서 넘겨준 헤더 값 추출
        String userIdStr = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role"); // 예: "SELLER"

        if (StringUtils.hasText(userIdStr) && StringUtils.hasText(role)) {
            try {
                UUID userId = UUID.fromString(userIdStr);

                // ROLE_ 접두사 없이 헤더 값("SELLER") 그대로 권한 부여 (hasAuthority와 매칭됨)
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (IllegalArgumentException e) {
                logger.warn("잘못된 형식의 X-User-Id 헤더가 전달되었습니다: " + userIdStr);
            }
        }

        filterChain.doFilter(request, response);
    }
}
