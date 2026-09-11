package com.parut.order.order.application.port.in;

import java.util.Optional;
import java.util.UUID;

import com.parut.order.order.application.port.in.dto.OrderItemSnapshotView;
import com.parut.order.order.application.port.in.dto.OrderSnapshotView;

// Order가 제공하는 주문/아이템 스냅샷 조회 포트 (결제 도메인이 사용)
public interface OrderSnapshotQueryUseCase {

    Optional<OrderSnapshotView> getOrderSnapshot(UUID orderId);

    Optional<OrderItemSnapshotView> getFirstOrderItemSnapshot(UUID orderId);
}
