package com.parut.product.timedeal.infrastructure.persistence.timedealpurchase;

import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;


public interface JpaTimeDealPurchaseRepository extends JpaRepository<TimeDealPurchase, UUID> {

    Optional<TimeDealPurchase> findByOrderId(UUID orderId);

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