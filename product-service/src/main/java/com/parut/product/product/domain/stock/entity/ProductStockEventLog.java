package com.parut.product.product.domain.stock.entity;

import com.parut.product.global.common.entity.BaseEntity;
import com.parut.product.product.domain.stock.enums.StockEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "p_product_stock_event_logs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_item_id", "event_type"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductStockEventLog extends BaseEntity {

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private StockEventType eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processed;

    public static ProductStockEventLog create(UUID reservationId, UUID orderItemId, StockEventType eventType) {
        ProductStockEventLog eventLog = new ProductStockEventLog();
        eventLog.reservationId = reservationId;
        eventLog.orderItemId = orderItemId;
        eventLog.eventType = eventType;
        eventLog.processed = Instant.now();
        return eventLog;
    }
}
