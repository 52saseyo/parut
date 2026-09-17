package com.parut.order.payment.application.port.in;

import java.util.Optional;
import java.util.UUID;

import com.parut.order.payment.application.port.in.dto.PaymentCancelReceipt;
import com.parut.order.payment.application.port.in.dto.PaymentCancelView;

// Payment가 제공하는 결제 취소 포트 (주문 취소, 환불 도메인이 사용)
public interface PaymentCancelUseCase {

    // PG 부분 취소를 실행
    Optional<PaymentCancelReceipt> cancelOnPg(UUID orderId, long cancelAmount, String reason);

    // PG 취소 결과를 결제 원장(잔액, 거래 이력)에 반영
    PaymentCancelView applyCancellation(UUID orderId, PaymentCancelReceipt receipt);
}
