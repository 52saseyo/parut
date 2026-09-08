package com.parut.order.payment.application.port.in;

import java.util.Optional;
import java.util.UUID;

import com.parut.order.payment.application.port.in.dto.PaymentView;

// Payment가 제공하는 결제 조회 포트 (Order 주문 상세 조회, 환불 도메인이 사용)
public interface PaymentQueryUseCase {

    Optional<PaymentView> getPayment(UUID orderId);
}
