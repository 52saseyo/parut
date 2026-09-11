package com.parut.user.global.config;

import com.parut.user.auth.infrastructure.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 비밀번호를 안전하게 암호화하는 BCrypt 구현체 사용
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 인증/인가 없이 누구나 접근 가능한 API
                        .requestMatchers("/api/v1/auth/**", "/actuator/**", "/api/v1/sellers/apply").permitAll()

                        // 2. 미승인 판매자(PENDING_SELLER)도 접근 가능한 입점 신청 조회 API
                        .requestMatchers("/api/v1/sellers/me/application").hasAnyAuthority("SELLER", "PENDING_SELLER")

                        // 3. 그 외 나머지 API는 오직 정상 승인된 SELLER만 접근 가능
                        .requestMatchers("/api/v1/products/**").hasRole("SELLER") // 예시: 상품 관련 API
                        .anyRequest().authenticated()
                )
                // 커스텀 JWT 필터를 UsernamePasswordAuthenticationFilter 이전에 동작하도록 설정
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}