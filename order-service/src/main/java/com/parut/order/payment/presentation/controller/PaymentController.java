package com.parut.order.payment.presentation.controller;

import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.constant.HeaderConstants;
import com.parut.order.payment.application.PaymentFacade;
import com.parut.order.payment.application.PaymentService;
import com.parut.order.payment.application.dto.PaymentConfirmResult;
import com.parut.order.payment.application.dto.PaymentReadyResult;
import com.parut.order.payment.presentation.dto.request.PaymentConfirmRequest;
import com.parut.order.payment.presentation.dto.request.PaymentReadyRequest;
import com.parut.order.payment.presentation.dto.response.PaymentConfirmResponse;
import com.parut.order.payment.presentation.dto.response.PaymentReadyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentFacade paymentFacade;

    @PostMapping("/ready")
    public ApiResponse<PaymentReadyResponse> ready(
            // TODO: 공통 인터셉터 개발시, userId, traceId 부분 수정 예정
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId,
            @Valid @RequestBody PaymentReadyRequest request
    ) {
        PaymentReadyResult result = paymentService.ready(request.toCommand(userId));

        return ApiResponse.success(PaymentReadyResponse.from(result), traceId);
    }

    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponse> confirm(
            // TODO: 공통 인터셉터 개발시, traceId 부분 수정 예정
            @RequestHeader(HeaderConstants.IDEMPOTENCY_KEY) String idempotencyKey,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResult result = paymentFacade.confirm(request.toCommand(idempotencyKey));

        return ApiResponse.success(PaymentConfirmResponse.from(result), traceId);
    }
}
