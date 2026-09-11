package com.parut.order.order.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.parut.order.order.domain.OrderCancel;

public interface OrderCancelRepository extends JpaRepository<OrderCancel, UUID> {

    List<OrderCancel> findByOrderId(UUID orderId);
}
