package com.parut.product.timedeal.infrastructure.persistence.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaTimeDealRepository extends JpaRepository<TimeDeal, UUID> {

    Optional<TimeDeal> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select timeDeal
              from TimeDeal timeDeal
             where timeDeal.id = :id
               and timeDeal.deletedAt is null
            """)
    Optional<TimeDeal> findByIdForUpdate(@Param("id") UUID id);


    // 시작 시각에 도달했고 아직 판매 기간 안인 예정 타임딜.
    @Query(value = """
            select id from product_schema.p_time_deals
             where deleted_at is null
               and (cast(:afterId as uuid) is null or id > cast(:afterId as uuid))
               and status = 'SCHEDULED'
               and start_at <= :now
               and end_at > :now
             order by id
             limit :limit
            """, nativeQuery = true)
    List<UUID> findTimeDealsToActivate(
            @Param("now") Instant now,
            @Param("afterId") UUID afterId,
            @Param("limit") int limit);

    // 오픈을 놓친 SCHEDULED도 종료 대상에 포함한다.
    @Query(value = """
            select id from product_schema.p_time_deals
             where deleted_at is null
               and (cast(:afterId as uuid) is null or id > cast(:afterId as uuid))
               and status in ('SCHEDULED', 'ACTIVE')
               and end_at <= :now
             order by id
             limit :limit
            """, nativeQuery = true)
    List<UUID> findTimeDealsToEnd(
            @Param("now") Instant now,
            @Param("afterId") UUID afterId,
            @Param("limit") int limit);
}
