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

    boolean existsByOrderId(UUID orderId);

    /**
     * 특정 사용자가 특정 타임딜에서 확보한 수량의 합.
     * 합산 대상 상태는 호출자가 넘긴다 — 어댑터가 RESERVED와 CONFIRMED만 전달하고 CANCELLED는 제외한다.
     * 이력이 하나도 없으면 sum이 null이 되므로 coalesce로 0을 돌려준다. 이 처리가 없으면
     * 첫 구매인 사용자에게서 NPE가 난다.
     */
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