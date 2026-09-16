package com.parut.order.payment.presentation.controller;

import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
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

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentFacade paymentFacade;

    @PostMapping("/ready")
    @RequireRole(UserRole.CUSTOMER)
    public ApiResponse<PaymentReadyResponse> ready(
            UserContext userContext,
            @Valid @RequestBody PaymentReadyRequest request
    ) {
        PaymentReadyResult result = paymentService.ready(request.toCommand(userContext.userId()));

        return ApiResponse.success(PaymentReadyResponse.from(result));
    }

    @PostMapping("/confirm")
    @RequireRole(UserRole.CUSTOMER)
    public ApiResponse<PaymentConfirmResponse> confirm(
            @RequestHeader(HeaderConstants.IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResult result = paymentFacade.confirm(request.toCommand(idempotencyKey));

        return ApiResponse.success(PaymentConfirmResponse.from(result));
    }
}
