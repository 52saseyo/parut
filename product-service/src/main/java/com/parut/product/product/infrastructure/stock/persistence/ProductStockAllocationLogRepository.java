package com.parut.product.product.infrastructure.stock.persistence;

import com.parut.product.product.domain.stock.entity.ProductStockAllocationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductStockAllocationLogRepository  extends JpaRepository<ProductStockAllocationLog, UUID> {
    List<ProductStockAllocationLog> findByStockIdIn(List<UUID> stockIds);
}
