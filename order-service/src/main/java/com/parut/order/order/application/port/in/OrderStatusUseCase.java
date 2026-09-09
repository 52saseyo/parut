package com.parut.order.order.application.port.in;

import java.time.Instant;
import java.util.UUID;

// Order가 제공하는 결제 흐름에 따른 주문 상태 전이 포트 (결제 도메인이 사용)
public interface OrderStatusUseCase {

    void markPaymentPending(UUID orderId, UUID actorId);

    void markPaid(UUID orderId, UUID actorId, Instant paidAt);

    void revertToStockReserved(UUID orderId, UUID actorId);
}
