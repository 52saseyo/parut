package com.parut.order.delivery.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByDeliveryGroupId(UUID deliveryGroupId);

    List<Delivery> findAllByStatusAndShippedAtLessThanEqual(
            DeliveryStatus status,
            OffsetDateTime shippedAt
    );
}
