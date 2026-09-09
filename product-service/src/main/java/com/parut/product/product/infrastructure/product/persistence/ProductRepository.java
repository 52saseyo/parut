package com.parut.product.product.infrastructure.product.persistence;

import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndSellerIdAndDeletedAtIsNull(UUID productId, UUID sellerId);
    boolean existsByIdAndSellerIdAndDeletedAtIsNull(UUID productId, UUID sellerId);
    Optional<Product> findByIdAndStatusInAndDeletedAtIsNull(UUID productId, Collection<ProductStatus> statuses);
    Optional<Product> findByIdAndDeletedAtIsNull(UUID productId);

    @Query("""
        select p.sellerId
        from Product p
        where p.id = :productId
          and p.deletedAt is null
        """)
    Optional<UUID> findSellerIdByProductId(
            @Param("productId") UUID productId
    );

    @Query("""
        select p.id
        from Product p
        where p.sellerId = :sellerId
          and p.deletedAt is null
        """)
    List<UUID> findProductIdsBySellerId(
            @Param("sellerId") UUID sellerId
    );


}
