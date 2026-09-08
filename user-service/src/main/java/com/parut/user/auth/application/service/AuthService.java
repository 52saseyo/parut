package com.parut.user.auth.application.service;

import com.parut.user.auth.application.dto.request.LoginRequest;
import com.parut.user.auth.application.dto.request.SignupRequest;
import com.parut.user.auth.application.dto.response.TokenResponse;
import com.parut.user.auth.infrastructure.JwtProvider;
import com.parut.user.global.exception.BusinessException;
import com.parut.user.global.exception.ErrorCode;
import com.parut.user.seller.domain.Seller;
import com.parut.user.seller.infrastructure.SellerRepository;
import com.parut.user.user.domain.User;
import com.parut.user.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void signup(SignupRequest request) {
        // 1. 아이디 중복 체크
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        // 2. 비밀번호 암호화 및 User 엔티티 생성 (User 클래스에 생성자/빌더 필요)
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(
                request.username(),
                encodedPassword,
                request.name(),
                request.slackId(),
                request.username() // createdBy
        );

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // 1. 회원 조회
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PWD_NOT_MATCH); // 비밀번호 불일치
        }

        // 3. 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.getId(), "CUSTOMER");
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 4. Redis에 RefreshToken 저장 로직 추가 (생략)
        // Refresh Token Redis 저장 (TTL: 8시간 - JwtProvider에 설정된 시간과 동일하게 맞춰도 무방)
        redisTemplate.opsForValue().set("REFRESH:" + user.getId(), refreshToken, Duration.ofHours(8));

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public TokenResponse sellerLogin(LoginRequest request) {
        // 1. 판매자 조회
        Seller seller = sellerRepository.findByLoginId(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), seller.getPassword())) {
            throw new BusinessException(ErrorCode.PWD_NOT_MATCH); // 비밀번호 불일치
        }

        // 3. 승인 상태 검증 (PENDING, REJECTED 상태면 로그인 차단)
        if (seller.getStatus() != com.parut.user.seller.domain.SellerStatus.APPROVED) {
            throw new BusinessException(ErrorCode.SELLER_ACCESS_DENIED); // 필요 시 "승인 대기 중입니다" 전용 에러 추가
        }

        // 4. 판매자 전용 토큰 생성
        String accessToken = jwtProvider.createAccessToken(seller.getId(), "SELLER");
        String refreshToken = jwtProvider.createRefreshToken(seller.getId());

        redisTemplate.opsForValue().set("REFRESH:" + seller.getId(), refreshToken, Duration.ofHours(8));

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        UUID userId = jwtProvider.getUserId(refreshToken);
        String savedRefreshToken = redisTemplate.opsForValue().get("REFRESH:" + userId);

        // Redis에 저장된 토큰이 없거나, 보낸 토큰과 일치하지 않으면 에러
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String role;

        if (userRepository.existsById(userId)) {
            role = "CUSTOMER";
        } else if (sellerRepository.existsById(userId)) {
            role = "SELLER";
        } else {
            // DB에 존재하지 않는 탈퇴한 회원이거나 유효하지 않은 UUID인 경우
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 새로운 Access Token 발급 (Refresh Token은 그대로 유지)
        String newAccessToken = jwtProvider.createAccessToken(userId, role);
        String newRefreshToken = jwtProvider.createRefreshToken(userId); // 프로젝트 내 Refresh Token 생성 메서드명 확인 필요

        // Redis에 새로운 Refresh Token으로 갱신 (기존 토큰 덮어쓰기)
        // 만료 시간 설정 방식(Duration 등)은 기존 로그인 로직에서 사용 중이신 방식을 그대로 적용해 주세요.
        redisTemplate.opsForValue().set(
                "REFRESH:" + userId,
                newRefreshToken,
                jwtProvider.getExpiration(newRefreshToken), // 만료 시간(Duration 또는 밀리초)
                TimeUnit.MILLISECONDS
        );

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String accessToken) {
        String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;

        if (!jwtProvider.validateToken(token)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        UUID userId = jwtProvider.getUserId(token);
        long expiration = jwtProvider.getExpiration(token);

        // Redis에서 Refresh Token 삭제 (더 이상 재발급 불가)
        redisTemplate.delete("REFRESH:" + userId);

        // Access Token 남은 시간만큼 Redis에 Blacklist로 등록
        redisTemplate.opsForValue().set("LOGOUT:" + token, "logout", Duration.ofMillis(expiration));
    }

    public TokenResponse loginAdmin() {
        // 테스트용 고정 혹은 동적 관리자 UUID 생성
        UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String role = "ADMIN";

        // JWT Provider를 통해 ADMIN 권한이 담긴 토큰 생성
        String accessToken = jwtProvider.createAccessToken(adminId, role);
        String refreshToken = jwtProvider.createRefreshToken(adminId);

        return new TokenResponse(accessToken, refreshToken);
    }
}
