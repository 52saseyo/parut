package com.parut.product.product.infrastructure.stock.persistence;

import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductStockReservationRepository extends JpaRepository<ProductStockReservation, UUID> {
    List<ProductStockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant expiresAt);
}
