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


    @Query(value = """
            select id from product_schema.p_time_deals
             where deleted_at is null
               and status in ('ACTIVE', 'SCHEDULED')
               and start_at <= :now
               and end_at > :now
             order by id
            """, nativeQuery = true)
    List<UUID> findTimeDealsAvailableForRedis(@Param("now") Instant now);
}
