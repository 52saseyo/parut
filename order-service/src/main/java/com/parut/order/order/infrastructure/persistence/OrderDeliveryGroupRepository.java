package com.parut.order.order.infrastructure.persistence;

import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderDeliveryGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderDeliveryGroupRepository extends JpaRepository<OrderDeliveryGroup, UUID> {

    List<OrderDeliveryGroup> findByOrderId(UUID orderId);

    boolean existsBySellerIdAndGroupStatus(UUID sellerId, DeliveryGroupStatus groupStatus);

    // 배송 단건 조회의 구매자 권한 검증을 위해 그룹이 속한 주문의 userId를 확인한다.
    @Query("""
            SELECT COUNT(g) > 0
            FROM OrderDeliveryGroup g JOIN Order o ON o.id = g.orderId
            WHERE g.id = :deliveryGroupId AND o.userId = :userId
            """)
    boolean isOwnedByCustomer(@Param("deliveryGroupId") UUID deliveryGroupId, @Param("userId") UUID userId);

    void deleteByOrderId(UUID orderId);
}
