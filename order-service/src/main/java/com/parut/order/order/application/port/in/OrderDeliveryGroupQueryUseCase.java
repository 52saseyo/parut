package com.parut.order.order.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.parut.order.order.application.port.in.dto.OrderDeliveryGroupView;

// Order가 제공하는 배송 그룹 조회 포트 (배송 도메인이 사용)
public interface OrderDeliveryGroupQueryUseCase {

    List<OrderDeliveryGroupView> getDeliveryGroups(UUID orderId);

    Optional<OrderDeliveryGroupView> getDeliveryGroup(UUID deliveryGroupId);
}
