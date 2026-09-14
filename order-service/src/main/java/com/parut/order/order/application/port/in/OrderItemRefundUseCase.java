package com.parut.order.order.application.port.in;

import java.util.UUID;

/**
 * 환불 처리에 따른 주문상품 상태 변경을 제공한다.
 */
public interface OrderItemRefundUseCase {

    /** ORDERED 상태일 때만 REFUND_REQUESTED로 변경한다. */
    void markRefundRequested(UUID orderItemId);

    /** REFUND_REQUESTED 상태일 때만 ORDERED로 되돌린다. */
    void cancelRefundRequest(UUID orderItemId);
}
