package com.parut.order.refund.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.common.ApiResponse;
import com.parut.order.refund.application.RefundFacade;
import com.parut.order.refund.application.RefundService;
import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.presentation.dto.request.ApproveRefundRequest;
import com.parut.order.refund.presentation.dto.request.RejectRefundRequest;
import com.parut.order.refund.presentation.dto.request.RequestRefundRequest;
import com.parut.order.refund.presentation.dto.response.RefundResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final RefundFacade refundFacade;

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
