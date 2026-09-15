package com.parut.order.order.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * 환불 처리에 따른 주문상품 상태 변경을 제공한다.
 */
public interface OrderItemRefundUseCase {

    /** ORDERED 상태일 때만 REFUND_REQUESTED로 변경한다. */
    void markRefundRequested(UUID orderItemId);

    /** REFUND_REQUESTED 상태일 때만 ORDERED로 되돌린다. */
    void cancelRefundRequest(UUID orderItemId);

    /**
     * 모든 주문상품이 REFUND_REQUESTED 상태일 때 REFUNDED로 변경한다.
     * 하나라도 상태가 다르면 일부만 변경하지 않고 전체 처리를 거부한다.
     */
    void markRefunded(List<UUID> orderItemIds);

    /** REFUND_REQUESTED 상태를 CONFIRMED로 변경하고 확정 시각을 기록한다. */
    void confirmRejectedRefund(UUID orderItemId);
}
