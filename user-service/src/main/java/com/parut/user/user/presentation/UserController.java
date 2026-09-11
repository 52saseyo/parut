package com.parut.user.user.presentation;

import com.parut.user.global.exception.BusinessException;
import com.parut.user.global.exception.ErrorCode;
import com.parut.user.user.application.dto.request.UserUpdateRequest;
import com.parut.user.user.application.dto.response.UserDeleteResponse;
import com.parut.user.user.application.dto.response.UserResponse;
import com.parut.user.user.application.service.UserService;
import com.parut.user.global.common.ApiResponse;
import com.parut.user.global.common.OffsetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 1. 사용자 단건 조회
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Role") String role
    ) {
        // ADMIN 권한 체크 로직
        if (!"ADMIN".equalsIgnoreCase(role)) {
            // 공통 에러 응답이나 적절한 예외 처리 (예: CustomException 또는 HttpStatus.FORBIDDEN)
            throw new BusinessException(ErrorCode.USER_ACCESS_DENIED);
        }

        UserResponse response = userService.getUser(userId);

        // global 패키지에 TraceId와 Timestamp를 포함하는 공통 응답 래퍼(ApiResponse)를 만들어 감싸줍니다.
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 2. 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        // 헤더에서 추출한 내 ID를 사용하여 기존 서비스 로직을 그대로 호출합니다.
        UserResponse response = userService.getUser(userId);

        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 3. 사용자 목록 조회 및 검색 (Admin 전용)
    // 관리자 화면이므로 Offset 방식이 적합하다 판단하여 Offset방식으로 진행
    @GetMapping
    public ResponseEntity<ApiResponse<OffsetResponse<UserResponse>>> getUserList(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            @RequestHeader("X-User-Role") String role
    ) {
        // ADMIN 권한 체크 로직
        if (!"ADMIN".equalsIgnoreCase(role)) {
            // 공통 에러 응답이나 적절한 예외 처리 (예: CustomException 또는 HttpStatus.FORBIDDEN)
            throw new BusinessException(ErrorCode.USER_ACCESS_DENIED);
        }

        OffsetResponse<UserResponse> response = userService.getUserList(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }


    // 4. 사용자 정보 수정
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody UserUpdateRequest request
    ) {
        // 1. ADMIN 또는 CUSTOMER 권한 체크
        if (!"ADMIN".equalsIgnoreCase(role) && !"CUSTOMER".equalsIgnoreCase(role)) {
            throw new BusinessException(ErrorCode.USER_ACCESS_DENIED);
        }

        // 2. CUSTOMER인 경우, 본인 정보인지(id와 requesterId가 일치하는지) 체크
        if ("CUSTOMER".equalsIgnoreCase(role) && !id.equals(requesterId)) {
            throw new BusinessException(ErrorCode.USER_ACCESS_DENIED);
        }

        UserResponse response = userService.updateUser(id, requesterId, request);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }


    // 5. 사용자 탈퇴
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDeleteResponse>> deleteUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader("X-User-Role") String role
    ) {
        // 1. ADMIN 또는 CUSTOMER 권한 체크
        if (!"ADMIN".equalsIgnoreCase(role) && !"CUSTOMER".equalsIgnoreCase(role)) {
            throw new BusinessException(ErrorCode.USER_ACCESS_DENIED);
        }

        // 2. CUSTOMER인 경우, 본인 정보인지(id와 requesterId가 일치하는지) 체크
        if ("CUSTOMER".equalsIgnoreCase(role) && !id.equals(requesterId)) {
            throw new BusinessException(ErrorCode.USER_ACCESS_DENIED);
        }

        UserDeleteResponse response = userService.deleteUser(id, requesterId);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }
}