package com.parut.product.product.infrastructure.stock.persistence;

import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductStockReservationRepository extends JpaRepository<ProductStockReservation, UUID> {
    Page<ProductStockReservation> findByStatusAndExpiresAtBefore(
            ReservationStatus status, Instant expiresAt, Pageable pageable);
    @Query("""
        SELECT r FROM ProductStockReservation r
        WHERE r.status = :status
          AND r.expiresAt < :now
          AND (:cursorExpiresAt IS NULL
               OR r.expiresAt > :cursorExpiresAt
               OR (r.expiresAt = :cursorExpiresAt AND r.id > :cursorId))
        ORDER BY r.expiresAt ASC, r.id ASC
        """)
    List<ProductStockReservation> findNextExpiredBatch(
            @Param("status") ReservationStatus status,
            @Param("now") Instant now,
            @Param("cursorExpiresAt") Instant cursorExpiresAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
