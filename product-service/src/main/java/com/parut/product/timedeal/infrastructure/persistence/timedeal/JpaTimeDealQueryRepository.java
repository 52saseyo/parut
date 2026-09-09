package com.parut.product.timedeal.infrastructure.persistence.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;


public interface JpaTimeDealQueryRepository extends JpaRepository<TimeDeal, UUID> {

    // NOTE: 엔티티를 로드하지 않고 View를 직접 채운다. 삭제된 타임딜은 조회 단계에서 걸러낸다.
    @Query("""
            select new com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView(
                       t.id,
                       t.productId,
                       t.sellerId,
                       t.imageId,
                       t.name,
                       t.originalPrice,
                       t.dealPrice,
                       t.productGrade,
                       t.origin,
                       t.harvestedDate)
              from TimeDeal t
             where t.id = :timeDealId
               and t.deletedAt is null
            """)
    Optional<TimeDealDetailView> findDetailById(@Param("timeDealId") UUID timeDealId);
}
