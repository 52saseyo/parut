package com.parut.user.seller.presentation;

import com.parut.user.global.exception.BusinessException;
import com.parut.user.global.exception.ErrorCode;
import com.parut.user.seller.application.dto.response.SellerDeleteResponse;
import com.parut.user.seller.application.service.SellerService;
import com.parut.user.seller.application.dto.request.SellerApplicationProcessRequest;
import com.parut.user.seller.application.dto.request.SellerApplyRequest;
import com.parut.user.seller.application.dto.request.SellerUpdateRequest;
import com.parut.user.seller.application.dto.response.SellerApplicationStatusResponse;
import com.parut.user.seller.application.dto.response.SellerResponse;
import com.parut.user.global.common.ApiResponse;
import com.parut.user.global.common.OffsetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    // 1. 판매자 입점 신청 - 신규 판매자 회원가입 및 입점 신청 처리 (승인 대기 상태로 생성)
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<SellerResponse>> apply(
            @RequestBody SellerApplyRequest request
    ) {
        SellerResponse response = sellerService.apply(request);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 2. 입점 신청 상태 조회 - 판매자 본인의 입점(가입) 신청 처리 상태 및 거절 사유 조회
    @GetMapping("/me/application")
    public ResponseEntity<ApiResponse<SellerApplicationStatusResponse>> getApplicationStatus(
            @RequestHeader("X-User-Id") UUID sellerId
    ) {
        SellerApplicationStatusResponse response = sellerService.getApplicationStatus(sellerId);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 3. [관리자] 가입 신청 목록 조회 - 관리자(Admin) 전용 판매자 입점 신청 목록 조회 및 검색 (페이징 지원)
    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<OffsetResponse<SellerResponse>>> getApplicationList(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            @RequestHeader("X-User-Role") String role
    ) {
        // ADMIN 권한 체크 로직
        if (!"ADMIN".equalsIgnoreCase(role)) {
            // 공통 에러 응답이나 적절한 예외 처리 (예: CustomException 또는 HttpStatus.FORBIDDEN)
            throw new BusinessException(ErrorCode.SELLER_ACCESS_DENIED);
        }
        OffsetResponse<SellerResponse> response = sellerService.getApplicationList(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 4. [관리자] 가입 신청 승인/반려 - 관리자(Admin)가 판매자의 입점(가입) 신청을 승인하거나 반려 처리
    @PatchMapping("/applications/{applicationId}")
    public ResponseEntity<ApiResponse<SellerResponse>> processApplication(
            @PathVariable UUID applicationId,
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody SellerApplicationProcessRequest request
    ) {
        // ADMIN 권한 체크 로직
        if (!"ADMIN".equalsIgnoreCase(role)) {
            // 공통 에러 응답이나 적절한 예외 처리 (예: CustomException 또는 HttpStatus.FORBIDDEN)
            throw new BusinessException(ErrorCode.SELLER_ACCESS_DENIED);
        }
        SellerResponse response = sellerService.processApplication(applicationId, request, "admin");
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 5. 판매자 내 정보 조회 - 로그인한 판매자의 본인 계정 및 업체 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SellerResponse>> getMyInfo(
            @RequestHeader("X-User-Id") UUID sellerId
    ) {
        SellerResponse response = sellerService.getSeller(sellerId);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 6. 판매자 내 정보 수정 - 로그인한 판매자의 본인 업체 및 담당자 정보 일부 수정
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerResponse>> updateMyInfo(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID sellerId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody SellerUpdateRequest request
    ) {
        // 1. ADMIN 또는 SELLER 권한 체크
        if (!"ADMIN".equalsIgnoreCase(role) && !"SELLER".equalsIgnoreCase(role) && !"PENDING_SELLER".equalsIgnoreCase(role)) {
            throw new BusinessException(ErrorCode.SELLER_ACCESS_DENIED);
        }

        // 2. SELLER인 경우, 본인 정보인지(id와 requesterId가 일치하는지) 체크
        if (("SELLER".equalsIgnoreCase(role) || "PENDING_SELLER".equalsIgnoreCase(role)) && !id.equals(sellerId)) {
            throw new BusinessException(ErrorCode.SELLER_ACCESS_DENIED);
        }

        SellerResponse response = sellerService.updateSeller(id, sellerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 7. 판매자 탈퇴 요청 - 로그인한 판매자 본인의 입점 탈퇴 처리 (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerDeleteResponse>> deleteMyInfo(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID sellerId,
            @RequestHeader("X-User-Role") String role
    ) {
        // 1. ADMIN 또는 SELLER 권한 체크
        if (!"ADMIN".equalsIgnoreCase(role) && !"SELLER".equalsIgnoreCase(role) && !"PENDING_SELLER".equalsIgnoreCase(role)) {
            throw new BusinessException(ErrorCode.SELLER_ACCESS_DENIED);
        }

        // 2. SELLER인 경우, 본인 정보인지(id와 requesterId가 일치하는지) 체크
        if (("SELLER".equalsIgnoreCase(role) || "PENDING_SELLER".equalsIgnoreCase(role)) && !id.equals(sellerId)) {
            throw new BusinessException(ErrorCode.SELLER_ACCESS_DENIED);
        }
        SellerDeleteResponse response = sellerService.deleteSeller(id, sellerId);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }
}