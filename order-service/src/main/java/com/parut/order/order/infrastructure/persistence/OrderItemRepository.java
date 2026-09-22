package com.parut.order.order.infrastructure.persistence;

import com.parut.order.order.domain.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    List<OrderItem> findByCancelId(UUID cancelId);

    int countByDeliveryGroupIdAndItemStatus(UUID deliveryGroupId, OrderItemStatus itemStatus);

    // 판매자 탈퇴 검증(미처리 주문 조회)을 위해 배송그룹의 sellerId와 조인한다.
    @Query("""
            SELECT COUNT(item) > 0
            FROM OrderItem item JOIN OrderDeliveryGroup g ON g.id = item.deliveryGroupId
            WHERE g.sellerId = :sellerId AND item.itemStatus = :status
            """)
    boolean existsBySellerIdAndItemStatus(@Param("sellerId") UUID sellerId, @Param("status") OrderItemStatus status);

    @Query("""
            SELECT item.id FROM OrderItem item
            WHERE item.itemStatus = :status
              AND item.id > :cursorId
            ORDER BY item.id
            """)
    List<UUID> findAutoConfirmationCandidateIds(
            @Param("status") OrderItemStatus status,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    void deleteByOrderId(UUID orderId);

    // 결제 전 전체 취소(TTL 자동취소)로 종료된 주문은 구매자 목록에서 제외한다.
    @Query("""
            SELECT item
              FROM OrderItem item
              JOIN Order o ON o.id = item.orderId
             WHERE o.userId = :userId
               AND (:itemStatus IS NULL OR item.itemStatus = :itemStatus)
               AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
               AND (:orderType IS NULL OR o.orderType = :orderType)
               AND o.orderedAt >= :startDate
               AND o.orderedAt <= :endDate
               AND NOT EXISTS (
                   SELECT 1 FROM OrderCancel oc
                    WHERE oc.orderId = item.orderId
                      AND oc.cancelReasonCode = :ttlReasonCode
               )
               AND (item.createdAt < :cursor
                    OR (item.createdAt = :cursor AND item.id < :cursorId))
             ORDER BY item.createdAt DESC, item.id DESC
            """)
    List<OrderItem> findBuyerOrderItems(
            @Param("userId") UUID userId,
            @Param("itemStatus") OrderItemStatus itemStatus,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("orderType") OrderType orderType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("ttlReasonCode") CancelReasonCode ttlReasonCode,
            @Param("cursor") Instant cursor,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
