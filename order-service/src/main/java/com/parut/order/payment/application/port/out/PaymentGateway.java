package com.parut.order.payment.application.port.out;

import com.parut.order.payment.application.port.out.dto.PaymentApproveResult;
import com.parut.order.payment.application.port.out.dto.PaymentCancelResult;

// PG(토스페이먼츠) 연동 포트
public interface PaymentGateway {

    void ready(String tossOrderId, long amount);

    PaymentApproveResult approve(String paymentKey, String tossOrderId, long amount, String idempotencyKey);

    PaymentCancelResult cancel(String paymentKey, long cancelAmount, String reason);

    // 웹훅 검증용 — PG 원장의 현재 상태를 재조회한다 (Toss 응답의 status 원문, 예: "DONE")
    String getStatus(String paymentKey);
}
