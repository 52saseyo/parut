package com.parut.order.order.application.dto;

import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderItem;

// OrderFacade가 재고(Feign) 호출에 필요한 ID를 얻기 위한 dto
// ToDo: bulk 도입 시 수정 예정
public record CreatedOrder(
        Order order,
        OrderItem item
) {
}
