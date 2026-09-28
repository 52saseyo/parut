package com.parut.order.payment.infrastructure.gateway;

import com.parut.order.payment.application.port.out.PaymentGateway;
import com.parut.order.payment.application.port.out.dto.PaymentApproveResult;
import com.parut.order.payment.application.port.out.dto.PaymentCancelResult;
import com.parut.order.payment.domain.PaymentMethod;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

// Mock: HTTP 호출·재시도·타임아웃 없이 성공 응답만 즉시 반환
// toss.enabled=false일 때만 활성화
@Component
@ConditionalOnProperty(name = "toss.enabled", havingValue = "false")
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public void ready(String tossOrderId, long amount) {
        // Mock: 별도로 준비할 PG 세션이 없음
    }

    @Override
    public PaymentApproveResult approve(String paymentKey, String tossOrderId, long amount, String idempotencyKey) {
        // Mock: 카드 고정
        // 실제 PG는 사용자가 결제창에서 고른 수단을 승인 응답으로 확정
        return new PaymentApproveResult(
                PaymentMethod.CREDIT_CARD,
                Instant.now(),
                "https://mock-pg.parut.local/receipts/" + UUID.randomUUID(),
                "MOCK-TX-" + UUID.randomUUID()
        );
    }

    @Override
    public PaymentCancelResult cancel(String paymentKey, long cancelAmount, String reason) {
        return new PaymentCancelResult(
                Instant.now(),
                "MOCK-TX-" + UUID.randomUUID()
        );
    }

    @Override
    public String getStatus(String paymentKey) {
        // Mock 모드에서는 실제 PG 웹훅이 오지 않아 호출될 일이 없다
        return "DONE";
    }
}
