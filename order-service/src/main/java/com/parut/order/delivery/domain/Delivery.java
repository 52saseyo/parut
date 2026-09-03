package com.parut.order.delivery.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.parut.order.global.common.entity.UpdatableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends UpdatableEntity {

    @Column(name = "delivery_group_id", nullable = false)
    private UUID deliveryGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "tracking_number", length = 30)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static Delivery create(UUID deliveryGroupId) {
        if (deliveryGroupId == null) {
            throw new IllegalArgumentException("배송 그룹 ID는 필수입니다.");
        }

        Delivery delivery = new Delivery();
        delivery.deliveryGroupId = deliveryGroupId;
        delivery.status = DeliveryStatus.PREPARING;
        return delivery;
    }

    public void ship(String trackingNumber, OffsetDateTime shippedAt) {
        if (status != DeliveryStatus.PREPARING) {
            throw new IllegalStateException("배송 준비 상태에서만 배송을 시작할 수 있습니다.");
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalArgumentException("운송장 번호는 필수입니다.");
        }
        if (trackingNumber.length() > 30) {
            throw new IllegalArgumentException("운송장 번호는 30자를 초과할 수 없습니다.");
        }
        if (shippedAt == null) {
            throw new IllegalArgumentException("배송 시작 시각은 필수입니다.");
        }

        this.trackingNumber = trackingNumber;
        this.shippedAt = shippedAt;
        this.status = DeliveryStatus.SHIPPED;
    }

    public void complete(OffsetDateTime deliveredAt) {
        if (status != DeliveryStatus.SHIPPED) {
            throw new IllegalStateException("배송 중 상태에서만 배송을 완료할 수 있습니다.");
        }
        if (deliveredAt == null) {
            throw new IllegalArgumentException("배송 완료 시각은 필수입니다.");
        }
        if (deliveredAt.isBefore(shippedAt)) {
            throw new IllegalArgumentException("배송 완료 시각은 배송 시작 시각보다 빠를 수 없습니다.");
        }

        this.deliveredAt = deliveredAt;
        this.status = DeliveryStatus.DELIVERED;
    }
}
