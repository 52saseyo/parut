package com.parut.product.product.infrastructure.stock.persistence;

import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductStockReservationRepository extends JpaRepository<ProductStockReservation, UUID> {
    @Query("""
        SELECT r FROM ProductStockReservation r
        WHERE r.status = :status
          AND r.expiresAt < :now
        ORDER BY r.expiresAt ASC, r.id ASC
        """)
    List<ProductStockReservation> findFirstExpiredBatch(
            @Param("status") ReservationStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("""
        SELECT r FROM ProductStockReservation r
        WHERE r.status = :status
          AND r.expiresAt < :now
          AND (r.expiresAt > :cursorExpiresAt
               OR (r.expiresAt = :cursorExpiresAt AND r.id > :cursorId))
        ORDER BY r.expiresAt ASC, r.id ASC
        """)
    List<ProductStockReservation> findNextExpiredBatchByCursor(
            @Param("status") ReservationStatus status,
            @Param("now") Instant now,
            @Param("cursorExpiresAt") Instant cursorExpiresAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    // 격리 예약 조회 - ADMIN용 (전체)
    List<ProductStockReservation> findByStatus(ReservationStatus status);

    // 격리 예약 조회 - SELLER용 (본인 상품 범위로 제한)
    List<ProductStockReservation> findByStatusAndStockIdIn(ReservationStatus status, List<UUID> stockIds);
}
