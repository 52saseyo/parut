package com.parut.order.delivery.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.parut.order.delivery.application.dto.DeliveryGroupSnapshot;

/**
 * Order가 Delivery에 제공하는 배송 그룹 조회 계약.
 *
 * <p>Delivery에서는 Order의 Repository를 직접 사용하지 않는다.
 */
public interface OrderDeliveryGroupQueryPort {

    List<DeliveryGroupSnapshot> findAllByOrderId(UUID orderId);

    Optional<DeliveryGroupSnapshot> findById(UUID deliveryGroupId);
}
