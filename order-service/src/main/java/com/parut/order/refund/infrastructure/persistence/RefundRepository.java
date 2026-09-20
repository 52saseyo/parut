package com.parut.order.refund.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.domain.RefundStatus;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    boolean existsByOrderItemIdAndStatusNot(
            UUID orderItemId,
            RefundStatus status
    );

    Page<Refund> findByStatus(RefundStatus status, Pageable pageable);

    // 역할별 소유자 컬럼을 선두 조건으로 유지해 각 커서 조회 인덱스를 사용한다.
    @Query("""
            select r
              from Refund r
             where r.customerId = :customerId
               and (:status is null or r.status = :status)
               and (:cursor is null
                    or r.createdAt < :cursor
                    or (r.createdAt = :cursor and r.id < :cursorId))
             order by r.createdAt desc, r.id desc
            """)
    List<Refund> findCustomerRefunds(
            @Param("customerId") UUID customerId,
            @Param("status") RefundStatus status,
            @Param("cursor") Instant cursor,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("""
            select r
              from Refund r
             where r.sellerId = :sellerId
               and (:status is null or r.status = :status)
               and (:cursor is null
                    or r.createdAt < :cursor
                    or (r.createdAt = :cursor and r.id < :cursorId))
             order by r.createdAt desc, r.id desc
            """)
    List<Refund> findSellerRefunds(
            @Param("sellerId") UUID sellerId,
            @Param("status") RefundStatus status,
            @Param("cursor") Instant cursor,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
