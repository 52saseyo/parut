package com.parut.order.order.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.parut.order.order.domain.OrderCancel;

public interface OrderCancelRepository extends JpaRepository<OrderCancel, UUID> {
}
