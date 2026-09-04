package com.parut.product.product.domain.stock.entity;

import com.parut.product.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processed;

    public static ProductStockEventLog create(UUID reservationId, UUID orderItemId, String eventType) {
        ProductStockEventLog eventLog = new ProductStockEventLog();
        eventLog.reservationId = reservationId;
        eventLog.orderItemId = orderItemId;
        eventLog.eventType = eventType;
        eventLog.processed = LocalDateTime.now();
        return eventLog;
    }
}
