package com.parut.order.order.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.parut.order.order.domain.OrderDeliveryGroup;

public interface OrderDeliveryGroupRepository extends JpaRepository<OrderDeliveryGroup, UUID> {

    List<OrderDeliveryGroup> findByOrderId(UUID orderId);
}
