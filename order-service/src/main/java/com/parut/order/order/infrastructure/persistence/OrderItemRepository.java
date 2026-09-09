package com.parut.order.order.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderItemStatus;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    int countByDeliveryGroupIdAndItemStatus(UUID deliveryGroupId, OrderItemStatus itemStatus);

    void deleteByOrderId(UUID orderId);
}
