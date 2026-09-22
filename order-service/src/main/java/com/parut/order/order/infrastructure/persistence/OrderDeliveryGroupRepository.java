package com.parut.order.order.infrastructure.persistence;

import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderDeliveryGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderDeliveryGroupRepository extends JpaRepository<OrderDeliveryGroup, UUID> {

    List<OrderDeliveryGroup> findByOrderId(UUID orderId);

    boolean existsBySellerIdAndGroupStatus(UUID sellerId, DeliveryGroupStatus groupStatus);

    void deleteByOrderId(UUID orderId);
}
