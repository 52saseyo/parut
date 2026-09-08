package com.parut.user.auth.presentation;

import com.parut.user.auth.application.service.AuthService;
import com.parut.user.auth.application.dto.request.LoginRequest;
import com.parut.user.auth.application.dto.request.SignupRequest;
import com.parut.user.auth.application.dto.response.TokenResponse;
import com.parut.user.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

    // 2. 일반 회원 로그인
    @PostMapping("/login/user")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    // 3. 판매자 로그인
    @PostMapping("/login/seller")
    public ResponseEntity<ApiResponse<TokenResponse>> sellerLogin(@RequestBody LoginRequest request) {
        TokenResponse response = authService.sellerLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    // 4. 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@RequestHeader("Refresh-Token") String refreshToken) {
        TokenResponse response = authService.reissue(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    // 5. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String accessToken) {
        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

    // 6. 관리자 계정 자동 생성
    @PostMapping("/login/admin")
    public ResponseEntity<ApiResponse<TokenResponse>> loginAdmin() {
        TokenResponse response = authService.loginAdmin();
        return ResponseEntity.ok(ApiResponse.success(response, "관리자 임시 토큰이 발급되었습니다."));
    }
}