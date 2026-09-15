package com.parut.product.product.infrastructure.stock.persistence;

import com.parut.product.product.domain.stock.entity.ProductStockEventLog;
import com.parut.product.product.domain.stock.enums.StockEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductStockEventLogRepository extends JpaRepository<ProductStockEventLog, UUID> {
    Optional<ProductStockEventLog> findByOrderItemIdAndEventType(UUID orderItemId, StockEventType eventType );
    Optional<ProductStockEventLog> findByReservationIdAndEventType(UUID reservationId, StockEventType eventType);
    List<ProductStockEventLog> findByOrderItemIdInAndEventType(List<UUID> orderItemIds, StockEventType eventType);
}
