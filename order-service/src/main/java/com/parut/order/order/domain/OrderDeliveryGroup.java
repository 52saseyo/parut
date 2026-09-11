package com.parut.order.order.domain;

import java.time.Instant;
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
@Table(name = "p_order_delivery_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderDeliveryGroup extends UpdatableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_status", nullable = false, length = 30)
    private DeliveryGroupStatus groupStatus;

    @Column(name = "product_amount", nullable = false)
    private Long productAmount;

    @Column(name = "delivery_fee", nullable = false)
    private Long deliveryFee;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static OrderDeliveryGroup create(
            UUID orderId,
            UUID sellerId,
            long productAmount,
            long deliveryFee
    ) {
        return new OrderDeliveryGroup(orderId, sellerId, productAmount, deliveryFee);
    }

    private OrderDeliveryGroup(
            UUID orderId,
            UUID sellerId,
            long productAmount,
            long deliveryFee
    ) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (sellerId == null) {
            throw new IllegalArgumentException("판매자 ID는 필수입니다.");
        }
        if (productAmount < 0) {
            throw new IllegalArgumentException("상품금액은 0 이상이어야 합니다.");
        }
        if (deliveryFee < 0) {
            throw new IllegalArgumentException("배송비는 0 이상이어야 합니다.");
        }

        this.orderId = orderId;
        this.sellerId = sellerId;
        this.groupStatus = DeliveryGroupStatus.PENDING;
        this.productAmount = productAmount;
        this.deliveryFee = deliveryFee;
    }

    public void markPreparing() {
        if (groupStatus != DeliveryGroupStatus.PENDING) {
            throw new IllegalStateException("대기 상태에서만 준비중으로 전이할 수 있습니다.");
        }

        this.groupStatus = DeliveryGroupStatus.PREPARING;
    }

    public void markShipped() {
        if (groupStatus != DeliveryGroupStatus.PREPARING) {
            throw new IllegalStateException("준비중 상태에서만 배송중으로 전이할 수 있습니다.");
        }

        this.groupStatus = DeliveryGroupStatus.SHIPPED;
    }

    public void markDelivered() {
        if (groupStatus != DeliveryGroupStatus.SHIPPED) {
            throw new IllegalStateException("배송중 상태에서만 배송완료로 전이할 수 있습니다.");
        }

        this.groupStatus = DeliveryGroupStatus.DELIVERED;
    }

    public void cancel(Instant canceledAt) {
        if (groupStatus != DeliveryGroupStatus.PENDING && groupStatus != DeliveryGroupStatus.PREPARING) {
            throw new IllegalStateException("대기·준비중 상태에서만 취소할 수 있습니다.");
        }
        if (canceledAt == null) {
            throw new IllegalArgumentException("취소 일시는 필수입니다.");
        }

        this.groupStatus = DeliveryGroupStatus.CANCELED;
        this.canceledAt = canceledAt;
    }
}
