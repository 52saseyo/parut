package com.parut.order.order.application.dto;

import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderItem;

import java.util.List;

// OrderFacade가 재고(Feign) 호출에 필요한 ID를 얻기 위한 dto
public record CreatedOrder(
        Order order,
        List<OrderItem> items
) {
}
