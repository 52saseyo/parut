package com.parut.order.payment.application.port.out;

import com.parut.order.payment.application.port.out.dto.PaymentApproveResult;
import com.parut.order.payment.application.port.out.dto.PaymentCancelResult;

// PG(토스페이먼츠) 연동 포트
// ToDo: MockPaymentGateway로 초기 연동, 추후 TossPaymentGateway로 교체
public interface PaymentGateway {

    void ready(String tossOrderId, long amount);

    PaymentApproveResult approve(String paymentKey, String tossOrderId, long amount, String idempotencyKey);

    PaymentCancelResult cancel(String paymentKey, long cancelAmount, String reason);
}
