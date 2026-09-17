package com.parut.order.order.application.port.in;

import java.util.List;
import java.util.UUID;

import com.parut.order.order.application.port.in.dto.OrderItemView;

// Order가 제공하는 주문상품 조회 포트 (환불 도메인이 사용)
public interface OrderItemQueryUseCase {

    // 요청한 ID 중 존재하는 것만 반환한다.
    List<OrderItemView> getOrderItems(List<UUID> orderItemIds);
}
