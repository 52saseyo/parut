package com.parut.order.delivery.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByDeliveryGroupId(UUID deliveryGroupId);

    // 역할별 소유자 컬럼을 선두 조건으로 유지해 각 커서 조회 인덱스를 사용한다.
    @Query("""
            select d
              from Delivery d
             where d.customerId = :customerId
               and (cast(:orderId as uuid) is null or d.orderId = :orderId)
               and (cast(:status as string) is null or d.status = :status)
               and (cast(:cursor as timestamp) is null
                    or d.createdAt < :cursor
                    or (d.createdAt = :cursor and d.id < :cursorId))
             order by d.createdAt desc, d.id desc
            """)
    List<Delivery> findCustomerDeliveries(
            @Param("customerId") UUID customerId,
            @Param("orderId") UUID orderId,
            @Param("status") DeliveryStatus status,
            @Param("cursor") Instant cursor,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("""
            select d
              from Delivery d
             where d.sellerId = :sellerId
               and (cast(:orderId as uuid) is null or d.orderId = :orderId)
               and (cast(:status as string) is null or d.status = :status)
               and (cast(:cursor as timestamp) is null
                    or d.createdAt < :cursor
                    or (d.createdAt = :cursor and d.id < :cursorId))
             order by d.createdAt desc, d.id desc
            """)
    List<Delivery> findSellerDeliveries(
            @Param("sellerId") UUID sellerId,
            @Param("orderId") UUID orderId,
            @Param("status") DeliveryStatus status,
            @Param("cursor") Instant cursor,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("""
            select d
              from Delivery d
             where (cast(:customerId as uuid) is null or d.customerId = :customerId)
               and (cast(:sellerId as uuid) is null or d.sellerId = :sellerId)
               and (cast(:orderId as uuid) is null or d.orderId = :orderId)
               and (cast(:status as string) is null or d.status = :status)
            """)
    Page<Delivery> findAdminDeliveries(
            @Param("customerId") UUID customerId,
            @Param("sellerId") UUID sellerId,
            @Param("orderId") UUID orderId,
            @Param("status") DeliveryStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT d.id FROM Delivery d
            WHERE d.status = :status
              AND d.shippedAt <= :threshold
              AND d.id > :cursorId
            ORDER BY d.id
            """)
    List<UUID> findEligibleIds(
            @Param("status") DeliveryStatus status,
            @Param("threshold") Instant threshold,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
