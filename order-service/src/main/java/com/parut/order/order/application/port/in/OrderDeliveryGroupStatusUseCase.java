package com.parut.order.order.application.port.in;

import java.util.UUID;

// Order가 제공하는 배송 그룹 상태 전이 포트 (배송, 결제 도메인이 사용)
// PREPARING -> SHIPPED 전이가 취소·환불의 경계
public interface OrderDeliveryGroupStatusUseCase {

    void markPreparing(UUID deliveryGroupId);

    void markShipped(UUID deliveryGroupId);

    void markDelivered(UUID deliveryGroupId);
}
