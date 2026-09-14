package com.parut.order.refund.presentation.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.constant.HeaderConstants;
import com.parut.order.refund.application.RefundService;
import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.presentation.dto.request.RequestRefundRequest;
import com.parut.order.refund.presentation.dto.response.RefundResponse;
import com.parut.order.refund.presentation.dto.request.RejectRefundRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/order-items/{orderItemId}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RefundResponse> requestRefund(
            @PathVariable UUID orderItemId,
            @RequestHeader(HeaderConstants.USER_ID) UUID customerId,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId,
            @RequestBody RequestRefundRequest request
    ) {
        Refund refund = refundService.requestRefund(
                orderItemId,
                customerId,
                request.reason()
        );

        return ApiResponse.success(RefundResponse.from(refund), traceId);
    }

    @PatchMapping("/refunds/{refundId}/cancel")
    public ApiResponse<RefundResponse> cancelRefund(
            @PathVariable UUID refundId,
            @RequestHeader(HeaderConstants.USER_ID) UUID customerId,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId
    ) {
        Refund refund = refundService.cancelRefund(refundId, customerId);
        return ApiResponse.success(RefundResponse.from(refund), traceId);
    }

    @PatchMapping("/refunds/{refundId}/reject")
    public ApiResponse<RefundResponse> rejectRefund(
            @PathVariable UUID refundId,
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId,
            @RequestBody RejectRefundRequest request
    ) {
        Refund refund = refundService.rejectRefund(refundId, sellerId, request.rejectionReason());
        return ApiResponse.success(RefundResponse.from(refund), traceId);
    }
}
