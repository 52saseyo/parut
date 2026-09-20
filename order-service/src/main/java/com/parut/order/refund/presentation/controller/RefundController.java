package com.parut.order.refund.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.common.CursorPageInfo;
import com.parut.order.global.common.CursorResponse;
import com.parut.order.global.common.OffsetPageInfo;
import com.parut.order.global.common.OffsetResponse;
import com.parut.order.global.common.SortDirection;
import com.parut.order.refund.application.RefundFacade;
import com.parut.order.refund.application.RefundPage;
import com.parut.order.refund.application.RefundService;
import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.domain.RefundStatus;
import com.parut.order.refund.presentation.dto.request.ApproveRefundRequest;
import com.parut.order.refund.presentation.dto.request.RejectRefundRequest;
import com.parut.order.refund.presentation.dto.request.RequestRefundRequest;
import com.parut.order.refund.presentation.dto.response.AdminRefundResponse;
import com.parut.order.refund.presentation.dto.response.RefundResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final RefundFacade refundFacade;

    /** 고객과 판매자에게 각자의 소유 범위에 속한 환불 한 건을 제공한다. */
    @GetMapping("/refunds/{refundId}")
    @RequireRole({UserRole.CUSTOMER, UserRole.SELLER})
    public ApiResponse<RefundResponse> getRefund(
            @PathVariable UUID refundId,
            UserContext userContext
    ) {
        Refund refund = refundService.getRefund(refundId, userContext.userId(), userContext.role());
        return ApiResponse.success(RefundResponse.from(refund));
    }

    /** 고객과 판매자의 환불 목록을 생성 시각 기준 커서 방식으로 제공한다. */
    @GetMapping("/refunds")
    @RequireRole({UserRole.CUSTOMER, UserRole.SELLER})
    public ApiResponse<CursorResponse<RefundResponse>> getRefunds(
            UserContext userContext,
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        RefundPage page = refundService.getRefunds(
                userContext.userId(),
                userContext.role(),
                status,
                cursor,
                cursorId,
                size
        );

        List<RefundResponse> content = page.content().stream()
                .map(RefundResponse::from)
                .toList();
        CursorPageInfo pageInfo = CursorPageInfo.of(
                page.nextCursor(),
                page.nextIdAfter(),
                page.hasNext(),
                "createdAt",
                SortDirection.DESC
        );

        return ApiResponse.success(new CursorResponse<>(content, pageInfo));
    }

    /** 관리자가 전체 환불을 운영 화면에서 조회할 수 있도록 오프셋 페이지를 제공한다. */
    @GetMapping("/admin/refunds")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<OffsetResponse<AdminRefundResponse>> getAdminRefunds(
            @RequestParam(required = false) RefundStatus status,
            @PageableDefault(
                    size = 10,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<Refund> page = refundService.getAdminRefunds(status, pageable);
        List<AdminRefundResponse> content = page.getContent().stream()
                .map(AdminRefundResponse::from)
                .toList();
        Sort.Order sortOrder = page.getSort().stream()
                .findFirst()
                .orElse(Sort.Order.desc("createdAt"));
        OffsetPageInfo pageInfo = OffsetPageInfo.of(
                page.getNumber(),
                page.getSize(),
                sortOrder.getProperty(),
                sortOrder.isAscending() ? SortDirection.ASC : SortDirection.DESC,
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );

        return ApiResponse.success(new OffsetResponse<>(content, pageInfo));
    }

    @PostMapping("/order-items/{orderItemId}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole(UserRole.CUSTOMER)
    public ApiResponse<RefundResponse> requestRefund(
            @PathVariable UUID orderItemId,
            UserContext userContext,
            @RequestBody RequestRefundRequest request
    ) {
        Refund refund = refundService.requestRefund(
                orderItemId,
                userContext.userId(),
                request.reason()
        );

        return ApiResponse.success(RefundResponse.from(refund));
    }

    @PatchMapping("/refunds/{refundId}/cancel")
    @RequireRole(UserRole.CUSTOMER)
    public ApiResponse<RefundResponse> cancelRefund(
            @PathVariable UUID refundId,
            UserContext userContext
    ) {
        Refund refund = refundService.cancelRefund(refundId, userContext.userId());
        return ApiResponse.success(RefundResponse.from(refund));
    }

    @PatchMapping("/refunds/approve")
    @RequireRole(UserRole.SELLER)
    public ApiResponse<List<RefundResponse>> approveRefunds(
            UserContext userContext,
            @RequestBody ApproveRefundRequest request
    ) {
        List<RefundResponse> refunds = refundFacade
                .approveRefunds(request.refundIds(), userContext.userId())
                .stream()
                .map(RefundResponse::from)
                .toList();

        return ApiResponse.success(refunds);
    }

    @PatchMapping("/refunds/{refundId}/reject")
    @RequireRole(UserRole.SELLER)
    public ApiResponse<RefundResponse> rejectRefund(
            @PathVariable UUID refundId,
            UserContext userContext,
            @RequestBody RejectRefundRequest request
    ) {
        Refund refund = refundService.rejectRefund(refundId, userContext.userId(), request.rejectionReason());
        return ApiResponse.success(RefundResponse.from(refund));
    }
}
