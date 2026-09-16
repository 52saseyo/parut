package com.parut.product.product.domain.stock.entity;

import com.parut.product.global.common.entity.BaseEntity;
import com.parut.product.product.domain.stock.enums.AllocationEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "p_product_stock_allocation_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductStockAllocationLog extends BaseEntity {
    @Column(name = "stock_id", nullable = false)
    private UUID stockId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AllocationEventType eventType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "processed_at", nullable = false)
    private Instant processed;

    public static ProductStockAllocationLog create(UUID stockId, AllocationEventType eventType, int quantity) {
        ProductStockAllocationLog log = new ProductStockAllocationLog();
        log.stockId = stockId;
        log.eventType = eventType;
        log.quantity = quantity;
        log.processed = Instant.now();
        return log;
    }
}
