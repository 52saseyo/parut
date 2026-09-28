package com.parut.order.payment.application;

import com.parut.order.payment.application.port.out.PaymentGateway;
import com.parut.order.payment.domain.Payment;
import com.parut.order.payment.infrastructure.persistence.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Toss 웹훅(PAYMENT_STATUS_CHANGED)으로 우리 흐름 밖에서 상태가 바뀌었는지 감지한다.
 *
 * <p>웹훅 본문은 서명이 없어 신뢰하지 않고 paymentKey로 Toss를 재조회한 결과만 사용한다.
 * 드리프트는 로그만 남기고 원장은 자동으로 바꾸지 않는다 — 운영자가 확인 후 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentWebhookService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public void handleStatusChanged(String paymentKey) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey).orElse(null);
        if (payment == null) {
            log.warn("[PaymentWebhookService] 알 수 없는 paymentKey로 웹훅 수신. paymentKey={}", paymentKey);
            return;
        }

        String tossStatus = paymentGateway.getStatus(paymentKey);
        if (tossStatus.equals(payment.getPaymentStatus().name())) {
            return;
        }

        log.warn("[PaymentWebhookService] 우리 흐름 밖에서 결제 상태가 변경됐습니다. 확인이 필요합니다. "
                        + "paymentId={}, ourStatus={}, tossStatus={}",
                payment.getId(), payment.getPaymentStatus(), tossStatus);
    }
}
