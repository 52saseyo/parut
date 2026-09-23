package com.parut.product.product.infrastructure.stock.persistence;

import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.enums.StockStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductStockRepository extends JpaRepository<ProductStock, UUID> {
    Optional<ProductStock> findByProductIdAndDeletedAtIsNull(UUID productId);
    List<ProductStock> findByProductIdInAndDeletedAtIsNull(List<UUID> productIds);
    @Query("""
        SELECT r FROM ProductStock r
        WHERE r.deletedAt IS NULL AND (:status IS NULL OR r.status = :status)
        """)
    Page<ProductStock> findByDeletedAtIsNull(StockStatus status, Pageable pageable);

    @Query("""
        SELECT r FROM ProductStock r
        WHERE r.productId IN :productIds AND r.deletedAt IS NULL AND (:status IS NULL OR r.status = :status)
        """)
    Page<ProductStock> findByProductIdInAndDeletedAtIsNull(List<UUID> productIds,StockStatus status, Pageable pageable);

    // reserve 전용 - 비관적 락
    // 여러 상품을 잠글 때 항상 id 순서로 잠가서 주문끼리 서로 기다리는 데드락을 막음
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s FROM ProductStock s
        WHERE s.productId IN :productIds AND s.deletedAt IS NULL
        ORDER BY s.id
        """)
    List<ProductStock> findByProductIdInForUpdate(List<UUID> productIds);

    // confirm/restore 전용 - reserve와 같은 순서(id)로 잠가 데드락 방지
// 상품 삭제 후에도 진행 중인 주문이 처리되도록 deletedAt 조건은 두지 않는다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s FROM ProductStock s
        WHERE s.id IN :ids
        ORDER BY s.id
        """)
    List<ProductStock> findAllByIdForUpdate(List<UUID> ids);
}
