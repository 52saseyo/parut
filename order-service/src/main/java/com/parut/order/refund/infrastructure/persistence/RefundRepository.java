package com.parut.order.refund.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.domain.RefundStatus;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    boolean existsByOrderItemIdAndStatusNot(
            UUID orderItemId,
            RefundStatus status
    );
}
