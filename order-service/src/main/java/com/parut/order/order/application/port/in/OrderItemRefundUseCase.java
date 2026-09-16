package com.parut.order.order.application.port.in;

import java.util.List;
import java.util.UUID;

// Order가 제공하는 주문상품 환불 상태 전이 포트 (환불 도메인이 사용). 모든 메서드는 목록 전체에 대한 all-or-nothing으로 동작한다.
public interface OrderItemRefundUseCase {

    // ORDERED -> REFUND_REQUESTED
    void requestRefund(List<UUID> orderItemIds);

    // REFUND_REQUESTED -> ORDERED (고객이 환불 요청을 철회)
    void withdrawRefundRequest(List<UUID> orderItemIds);

    // REFUND_REQUESTED -> REFUNDED
    void applyRefundCompletion(List<UUID> orderItemIds);

    // REFUND_REQUESTED -> CONFIRMED
    void rejectRefund(List<UUID> orderItemIds);
}
