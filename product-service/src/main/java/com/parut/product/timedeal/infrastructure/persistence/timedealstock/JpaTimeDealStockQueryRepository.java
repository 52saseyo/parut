package com.parut.product.timedeal.infrastructure.persistence.timedealstock;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockListView;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaTimeDealStockQueryRepository extends JpaRepository<TimeDealStock, UUID> {

    @Query("""
            select new com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockListView(
                       t.id, t.startAt,
                       s.availableQuantity, s.reservedQuantity, s.soldQuantity, s.lowStockThreshold)
              from TimeDeal t
              join TimeDealStock s on s.timeDealId = t.id
             where t.sellerId = :sellerId
               and t.deletedAt is null
               and s.deletedAt is null
             order by t.startAt desc, t.id desc
            """)
    List<TimeDealStockListView> findFirstSellerOwnedTimeDealStockList(
            @Param("sellerId") UUID sellerId,
            Pageable pageable
    );

    @Query("""
            select new com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockListView(
                       t.id, t.startAt,
                       s.availableQuantity, s.reservedQuantity, s.soldQuantity, s.lowStockThreshold)
              from TimeDeal t
              join TimeDealStock s on s.timeDealId = t.id
             where t.sellerId = :sellerId
               and t.deletedAt is null
               and s.deletedAt is null
               and (t.startAt < :cursor
                    or (t.startAt = :cursor and t.id < :cursorId))
             order by t.startAt desc, t.id desc
            """)
    List<TimeDealStockListView> findNextSellerOwnedTimeDealStockList(
            @Param("sellerId") UUID sellerId,
            @Param("cursor") Instant cursor,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
