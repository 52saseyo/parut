package com.parut.order.order.infrastructure.persistence;

import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderItemStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
