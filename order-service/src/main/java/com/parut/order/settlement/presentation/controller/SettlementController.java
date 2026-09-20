package com.parut.order.settlement.presentation.controller;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.common.CursorPageInfo;
import com.parut.order.global.common.CursorResponse;
import com.parut.order.global.common.SortDirection;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.settlement.application.SettlementPage;
import com.parut.order.settlement.application.SettlementService;
import com.parut.order.settlement.domain.SettlementStatus;
import com.parut.order.settlement.presentation.dto.request.CompleteSettlementsRequest;
import com.parut.order.settlement.presentation.dto.response.AdminSettlementResponse;
import com.parut.order.settlement.presentation.dto.response.SellerSettlementResponse;
import com.parut.order.settlement.presentation.dto.response.SettlementCompleteResponse;

import lombok.RequiredArgsConstructor;

/**
 * 판매자 정산 조회와 관리자 정산 대상 조회 및 완료 API를 제공한다.
 *
 * <p>역할 접근은 {@link RequireRole}로 제한하고, 판매자 목록은 인증된 판매자 ID를 조회 조건으로 사용한다.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/settlements")
    @RequireRole(UserRole.SELLER)
    public ApiResponse<CursorResponse<SellerSettlementResponse>> getSettlements(
            @RequestParam(defaultValue = "PENDING") SettlementStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "10") int size,
            UserContext userContext
    ) {
        validatePage(cursor, cursorId, size);
        SettlementPage page = settlementService.getSellerSettlements(
                userContext.userId(), status, parseCursor(cursor), cursorId, size);
        CursorResponse<SellerSettlementResponse> response = new CursorResponse<>(
                page.content().stream().map(SellerSettlementResponse::from).toList(),
                pageInfo(page));
        return ApiResponse.success(response);
    }

    @GetMapping("/settlement-targets")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<CursorResponse<AdminSettlementResponse>> getSettlementTargets(
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePage(cursor, cursorId, size);
        SettlementPage page = settlementService.getAdminSettlements(
                sellerId, parseCursor(cursor), cursorId, size);
        CursorResponse<AdminSettlementResponse> response = new CursorResponse<>(
                page.content().stream().map(AdminSettlementResponse::from).toList(), pageInfo(page));
        return ApiResponse.success(response);
    }

    @PatchMapping("/settlements/complete")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<List<SettlementCompleteResponse>> completeSettlements(
            @RequestBody CompleteSettlementsRequest request,
            UserContext userContext
    ) {
        Instant completionTime = Instant.now();
        List<SettlementCompleteResponse> response = settlementService.completeSettlements(
                        request.settlementIds(), userContext.userId(), completionTime)
                .stream()
                .map(SettlementCompleteResponse::from)
                .toList();
        return ApiResponse.success(response);
    }

    private CursorPageInfo pageInfo(SettlementPage page) {
        return CursorPageInfo.of(
                page.nextCursor() == null ? null : page.nextCursor().toString(),
                page.nextIdAfter(), page.hasNext(), "createdAt", SortDirection.DESC);
    }

    private void validatePage(String cursor, UUID cursorId, int size) {
        // 동일 생성 시각의 경계를 ID로 구분하므로 두 Cursor 값은 함께 전달되어야 한다.
        if ((cursor == null) != (cursorId == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (size != 10 && size != 30 && size != 50) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }
    }

    private Instant parseCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            return Instant.parse(cursor);
        } catch (DateTimeException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
