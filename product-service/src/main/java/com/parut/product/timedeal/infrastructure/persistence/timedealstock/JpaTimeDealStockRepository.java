package com.parut.product.timedeal.infrastructure.persistence.timedealstock;

import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;


public interface JpaTimeDealStockRepository extends JpaRepository<TimeDealStock, UUID> {

    Optional<TimeDealStock> findByTimeDealIdAndDeletedAtIsNull(UUID timeDealId);

    @Modifying
    @Query("""
            update TimeDealStock stock
               set stock.availableQuantity = stock.availableQuantity - :quantity,
                   stock.reservedQuantity = stock.reservedQuantity + :quantity
             where stock.timeDealId = :timeDealId
               and stock.deletedAt is null
               and stock.availableQuantity >= :quantity
            """)
    int reserveQuantityAtomically(
            @Param("timeDealId") UUID timeDealId,
            @Param("quantity") int quantity
    );
}
