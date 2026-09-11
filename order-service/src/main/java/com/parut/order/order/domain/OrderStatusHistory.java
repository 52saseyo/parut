package com.parut.order.order.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_order_status_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id; // 삽입 순서 보장을 위해 UUID 대신 BIGINT 사용

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private OrderStatus toStatus;

    @Column(name = "change_reason", length = 255)
    private String changeReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 50)
    private String createdBy;

    public static OrderStatusHistory record(
            UUID orderId,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String changeReason,
            String createdBy
    ) {
        return new OrderStatusHistory(orderId, fromStatus, toStatus, changeReason, createdBy);
    }

    private OrderStatusHistory(
            UUID orderId,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String changeReason,
            String createdBy
    ) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (toStatus == null) {
            throw new IllegalArgumentException("변경 상태는 필수입니다.");
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new IllegalArgumentException("생성자 ID는 필수입니다.");
        }

        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changeReason = changeReason;
        this.createdAt = Instant.now();
        this.createdBy = createdBy;
    }
}
