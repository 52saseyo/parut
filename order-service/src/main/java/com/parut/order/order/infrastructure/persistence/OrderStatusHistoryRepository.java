package com.parut.order.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.parut.order.order.domain.OrderStatusHistory;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
}
