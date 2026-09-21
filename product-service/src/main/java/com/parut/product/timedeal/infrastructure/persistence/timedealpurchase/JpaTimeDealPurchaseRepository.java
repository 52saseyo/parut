package com.parut.product.timedeal.infrastructure.persistence.timedealpurchase;

import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import org.springframework.data.domain.Pageable;


public interface JpaTimeDealPurchaseRepository extends JpaRepository<TimeDealPurchase, UUID> {

    Optional<TimeDealPurchase> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TimeDealPurchase p where p.orderId = :orderId")
    Optional<TimeDealPurchase> findByOrderIdForUpdate(@Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TimeDealPurchase p where p.id = :id")
    Optional<TimeDealPurchase> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select purchase
              from TimeDealPurchase purchase
             where purchase.deletedAt is null
               and purchase.status = com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus.RESERVED
               and purchase.expiresAt < :cutoff
             order by purchase.expiresAt asc, purchase.id asc
            """)
    List<TimeDealPurchase> findFirstExpiredReservationBatch(
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

    @Query("""
            select purchase
              from TimeDealPurchase purchase
             where purchase.deletedAt is null
               and purchase.status = com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus.RESERVED
               and purchase.expiresAt < :cutoff
               and (purchase.expiresAt > :cursorExpiresAt
                    or (purchase.expiresAt = :cursorExpiresAt and purchase.id > :cursorId))
             order by purchase.expiresAt asc, purchase.id asc
            """)
    List<TimeDealPurchase> findNextExpiredReservationBatchByCursor(
            @Param("cutoff") Instant cutoff,
            @Param("cursorExpiresAt") Instant cursorExpiresAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    // NOTE: 합산 대상 상태는 어댑터가 넘긴다. 이력이 없으면 sum이 null이라 coalesce로 0을 돌려준다
    // — 없으면 첫 구매 사용자에게서 NPE가 난다.
    @Query("""
            select coalesce(sum(p.quantity), 0)
              from TimeDealPurchase p
             where p.timeDealId = :timeDealId
               and p.userId = :userId
               and p.status in :statuses
            """)
    long sumQuantityByStatusIn(
            @Param("timeDealId") UUID timeDealId,
            @Param("userId") UUID userId,
            @Param("statuses") Collection<TimeDealPurchaseStatus> statuses
    );
}
