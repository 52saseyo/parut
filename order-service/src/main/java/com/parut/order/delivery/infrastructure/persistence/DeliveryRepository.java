package com.parut.order.delivery.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByDeliveryGroupId(UUID deliveryGroupId);

    @Query("""
            SELECT d.id FROM Delivery d
            WHERE d.status = :status
              AND d.shippedAt <= :threshold
              AND d.id > :cursorId
            ORDER BY d.id
            """)
    List<UUID> findEligibleIds(
            @Param("status") DeliveryStatus status,
            @Param("threshold") Instant threshold,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
