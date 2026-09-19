package com.parut.order.order.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderItemStatus;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    List<OrderItem> findByCancelId(UUID cancelId);

    int countByDeliveryGroupIdAndItemStatus(UUID deliveryGroupId, OrderItemStatus itemStatus);

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
