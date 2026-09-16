package com.parut.product.product.infrastructure.stock.persistence;

import com.parut.product.product.domain.stock.entity.ProductStockEventLog;
import com.parut.product.product.domain.stock.enums.StockEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductStockEventLogRepository extends JpaRepository<ProductStockEventLog, UUID> {
    Optional<ProductStockEventLog> findByOrderItemIdAndEventType(UUID orderItemId, StockEventType eventType );
    Optional<ProductStockEventLog> findByReservationIdAndEventType(UUID reservationId, StockEventType eventType);
    List<ProductStockEventLog> findByOrderItemIdInAndEventType(List<UUID> orderItemIds, StockEventType eventType);

    // stockId를 직접 들고 있지 않아 ProductStockReservation과 조인해서 stockId로 거름
    // (reservation 전체를 미리 안 가져와도 되게 하기 위함 - 인기 상품에서 예약이 많이 쌓여도 이 조회 자체는 size만큼만 나감)
    @Query("""
      SELECT e FROM ProductStockEventLog e, ProductStockReservation r
      WHERE e.reservationId = r.id
        AND r.stockId = :stockId
      ORDER BY e.createdAt DESC, e.id DESC
      """)
    List<ProductStockEventLog> findFirstHistoryBatch(
            @Param("stockId") UUID stockId,
            Pageable pageable
    );

    @Query("""
      SELECT e FROM ProductStockEventLog e, ProductStockReservation r
      WHERE e.reservationId = r.id
        AND r.stockId = :stockId
        AND (e.createdAt < :cursorCreatedAt
             OR (e.createdAt = :cursorCreatedAt AND e.id < :cursorId))
      ORDER BY e.createdAt DESC, e.id DESC
      """)
    List<ProductStockEventLog> findNextHistoryBatchByCursor(
            @Param("stockId") UUID stockId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
