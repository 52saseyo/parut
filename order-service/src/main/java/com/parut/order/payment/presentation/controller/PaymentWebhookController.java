package com.parut.order.payment.presentation.controller;

import com.parut.order.global.common.ApiResponse;
import com.parut.order.payment.application.PaymentWebhookService;
import com.parut.order.payment.presentation.dto.request.TossWebhookRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private static final String STATUS_CHANGED_EVENT_TYPE = "PAYMENT_STATUS_CHANGED";

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@RequestBody TossWebhookRequest request) {
        if (STATUS_CHANGED_EVENT_TYPE.equals(request.eventType()) && request.data() != null) {
            paymentWebhookService.handleStatusChanged(request.data().paymentKey());
        }

        return ApiResponse.success(null);
    }
}
