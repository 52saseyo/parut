package com.parut.product.product.infrastructure.stock.persistence;

import com.parut.product.product.domain.stock.entity.ProductStockAllocationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductStockAllocationLogRepository  extends JpaRepository<ProductStockAllocationLog, UUID> {

    @Query("""                                                                                                                                                                                                                      
      SELECT a FROM ProductStockAllocationLog a
      WHERE a.stockId IN :stockIds
      ORDER BY a.createdAt DESC, a.id DESC
      """)
    List<ProductStockAllocationLog> findFirstHistoryBatch(
            @Param("stockIds") List<UUID> stockIds,
            Pageable pageable
    );

    @Query("""                                                                                                                                                                                                                        
      SELECT a FROM ProductStockAllocationLog a
      WHERE a.stockId IN :stockIds
        AND (a.createdAt < :cursorCreatedAt
             OR (a.createdAt = :cursorCreatedAt AND a.id < :cursorId))
      ORDER BY a.createdAt DESC, a.id DESC
      """)
    List<ProductStockAllocationLog> findNextHistoryBatchByCursor(
            @Param("stockIds") List<UUID> stockIds,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
