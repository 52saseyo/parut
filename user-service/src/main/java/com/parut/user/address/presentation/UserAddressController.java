package com.parut.user.address.presentation;

import com.parut.user.address.application.dto.request.AddressCreateRequest;
import com.parut.user.address.application.dto.request.AddressUpdateRequest;
import com.parut.user.address.application.dto.response.AddressDeleteResponse;
import com.parut.user.address.application.dto.response.AddressResponse;
import com.parut.user.address.application.service.UserAddressService;
import com.parut.user.global.common.ApiResponse;
import com.parut.user.global.common.UserRole;
import com.parut.user.global.exception.BusinessException;
import com.parut.user.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    // 1. 배송지 목록 조회 - 로그인한 고객 본인의 저장 배송지 전체 조회 (기본 배송지 우선)
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role
    ) {
        verifyCustomer(role);

        List<AddressResponse> response = userAddressService.getAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 2. 배송지 등록 - 첫 배송지는 요청값과 관계없이 기본 배송지로 지정
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody AddressCreateRequest request
    ) {
        verifyCustomer(role);

        AddressResponse response = userAddressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, ""));
    }

    // 3. 배송지 부분 수정 - 생략한 값은 유지하며 기본 배송지 여부는 변경하지 않음
    @PatchMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable UUID addressId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody AddressUpdateRequest request
    ) {
        verifyCustomer(role);

        AddressResponse response = userAddressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 4. 기본 배송지 변경 - 기존 기본 배송지를 해제하고 선택한 배송지를 기본 배송지로 지정
    @PatchMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> changeDefaultAddress(
            @PathVariable UUID addressId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role
    ) {
        verifyCustomer(role);

        AddressResponse response = userAddressService.changeDefaultAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 5. 배송지 삭제 - 논리 삭제 후 필요하면 남은 최근 배송지를 기본 배송지로 재지정
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressDeleteResponse>> deleteAddress(
            @PathVariable UUID addressId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role
    ) {
        verifyCustomer(role);

        AddressDeleteResponse response = userAddressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(response, ""));
    }

    // 계정 배송지는 고객 본인만 사용할 수 있습니다.
    private void verifyCustomer(String role) {
        if (!String.valueOf(UserRole.CUSTOMER).equalsIgnoreCase(role)) {
            throw new BusinessException(ErrorCode.USER_ACCESS_DENIED);
        }
    }
}
