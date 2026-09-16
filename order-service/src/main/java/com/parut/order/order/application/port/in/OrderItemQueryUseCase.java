package com.parut.order.order.application.port.in;

import java.util.List;
import java.util.UUID;

import com.parut.order.order.application.port.in.dto.OrderItemView;

/**
 * 요청한 주문상품의 환불 및 정산 판단 정보를 제공한다.
 */
public interface OrderItemQueryUseCase {

    /** 요청한 ID 중 존재하는 주문상품 정보를 목록으로 반환한다. */
    List<OrderItemView> getOrderItems(List<UUID> orderItemIds);
}
