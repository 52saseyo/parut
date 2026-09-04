package com.parut.product.product.infrastructure.stock.persistence;

import com.parut.product.product.domain.stock.entity.ProductStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductStockRepository extends JpaRepository<ProductStock, UUID> {
    Optional<ProductStock> findByProductIdAndDeletedAtIsNull(UUID productId);
    List<ProductStock> findByProductIdInAndDeletedAtIsNull(List<UUID> productIds);
    Page<ProductStock> findByDeletedAtIsNull(Pageable pageable);

}
