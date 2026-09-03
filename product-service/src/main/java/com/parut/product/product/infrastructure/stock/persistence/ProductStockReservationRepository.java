package com.parut.product.product.infrastructure.stock.persistence;

import com.parut.product.product.domain.stock.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductStockReservationRepository extends JpaRepository<ProductStockRepository, UUID> {
    List<ProductStockRepository> findByStatusAndExpiresAtBefore(ReservationStatus status, UUID productId);
}
