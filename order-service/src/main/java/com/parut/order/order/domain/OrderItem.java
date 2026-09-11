package com.parut.order.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "p_order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends UpdatableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "delivery_group_id", nullable = false)
    private UUID deliveryGroupId;

    // 직접 등록 타임딜 주문은 NULL(참조할 원본 상품이 없음)
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "time_deal_id")
    private UUID timeDealId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    // Product Service의 AppearanceType Enum과 동일한 값 사용
    @Column(name = "appearance_type", length = 20)
    private String appearanceType;

    @Column(name = "origin", length = 50)
    private String origin;

    @Column(name = "harvest_date")
    private LocalDate harvestDate;

    @Column(name = "sale_unit", length = 20)
    private String saleUnit;

    @Column(name = "unit_quantity", precision = 10, scale = 2)
    private BigDecimal unitQuantity;

    @Column(name = "original_price")
    private Long originalPrice;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false, length = 30)
    private OrderItemStatus itemStatus;

    @Column(name = "cancel_id")
    private UUID cancelId;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static OrderItem create(
            UUID orderId,
            UUID deliveryGroupId,
            UUID productId,
            UUID timeDealId,
            String productName,
            String appearanceType,
            String origin,
            LocalDate harvestDate,
            String saleUnit,
            BigDecimal unitQuantity,
            Long originalPrice,
            long unitPrice,
            int quantity
    ) {
        return new OrderItem(
                orderId,
                deliveryGroupId,
                productId,
                timeDealId,
                productName,
                appearanceType,
                origin,
                harvestDate,
                saleUnit,
                unitQuantity,
                originalPrice,
                unitPrice,
                quantity
        );
    }

    private OrderItem(
            UUID orderId,
            UUID deliveryGroupId,
            UUID productId,
            UUID timeDealId,
            String productName,
            String appearanceType,
            String origin,
            LocalDate harvestDate,
            String saleUnit,
            BigDecimal unitQuantity,
            Long originalPrice,
            long unitPrice,
            int quantity
    ) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (deliveryGroupId == null) {
            throw new IllegalArgumentException("배송 그룹 ID는 필수입니다.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        // appearanceType, origin, harvestDate, saleUnit, unitQuantity, originalPrice는 스냅샷 부가 정보라
        // null은 허용하되, 값이 있으면 유효성은 검증한다.
        if (appearanceType != null && appearanceType.isBlank()) {
            throw new IllegalArgumentException("상품 속성은 빈 값일 수 없습니다.");
        }
        if (origin != null && origin.isBlank()) {
            throw new IllegalArgumentException("생산지는 빈 값일 수 없습니다.");
        }
        if (saleUnit != null && saleUnit.isBlank()) {
            throw new IllegalArgumentException("판매 단위는 빈 값일 수 없습니다.");
        }
        if (unitQuantity != null && unitQuantity.signum() <= 0) {
            throw new IllegalArgumentException("판매 단위 수량은 0보다 커야 합니다.");
        }
        if (originalPrice != null && originalPrice < 0) {
            throw new IllegalArgumentException("정가는 0 이상이어야 합니다.");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("판매단가는 0 이상이어야 합니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문수량은 0보다 커야 합니다.");
        }

        this.orderId = orderId;
        this.deliveryGroupId = deliveryGroupId;
        this.productId = productId;
        this.timeDealId = timeDealId;
        this.productName = productName;
        this.appearanceType = appearanceType;
        this.origin = origin;
        this.harvestDate = harvestDate;
        this.saleUnit = saleUnit;
        this.unitQuantity = unitQuantity;
        this.originalPrice = originalPrice;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.itemStatus = OrderItemStatus.ORDERED;
    }

    public void cancel(UUID cancelId) {
        if (itemStatus != OrderItemStatus.ORDERED) {
            throw new IllegalStateException("주문 상태에서만 취소할 수 있습니다.");
        }
        if (cancelId == null) {
            throw new IllegalArgumentException("취소 ID는 필수입니다.");
        }

        this.itemStatus = OrderItemStatus.CANCELED;
        this.cancelId = cancelId;
    }

    public void requestRefund() {
        if (itemStatus != OrderItemStatus.ORDERED) {
            throw new IllegalStateException("주문 상태에서만 환불을 요청할 수 있습니다.");
        }

        this.itemStatus = OrderItemStatus.REFUND_REQUESTED;
    }

    public void markRefunded() {
        if (itemStatus != OrderItemStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불 요청 상태에서만 환불 완료로 전이할 수 있습니다.");
        }

        this.itemStatus = OrderItemStatus.REFUNDED;
    }

    public void returnToOrdered() {
        if (itemStatus != OrderItemStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불 요청 상태에서만 주문 상태로 되돌릴 수 있습니다.");
        }

        this.itemStatus = OrderItemStatus.ORDERED;
    }

    public void confirm(Instant confirmedAt) {
        if (itemStatus != OrderItemStatus.ORDERED) {
            throw new IllegalStateException("주문 상태에서만 구매확정으로 전이할 수 있습니다.");
        }
        if (confirmedAt == null) {
            throw new IllegalArgumentException("구매확정 시각은 필수입니다.");
        }

        this.itemStatus = OrderItemStatus.CONFIRMED;
        this.confirmedAt = confirmedAt;
    }

    public boolean isCancelable(DeliveryGroupStatus groupStatus) {
        return itemStatus == OrderItemStatus.ORDERED
                && (groupStatus == DeliveryGroupStatus.PENDING || groupStatus == DeliveryGroupStatus.PREPARING);
    }

    public boolean isRefundable(DeliveryGroupStatus groupStatus) {
        return itemStatus == OrderItemStatus.ORDERED
                && (groupStatus == DeliveryGroupStatus.SHIPPED || groupStatus == DeliveryGroupStatus.DELIVERED);
    }
}
