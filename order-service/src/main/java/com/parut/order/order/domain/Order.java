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
@Table(name = "p_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends UpdatableEntity {

    @Column(name = "order_no", nullable = false, length = 30)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Column(name = "total_product_amount", nullable = false)
    private Long totalProductAmount;

    @Column(name = "total_delivery_fee", nullable = false)
    private Long totalDeliveryFee;

    @Column(name = "total_payment_amount", nullable = false)
    private Long totalPaymentAmount;

    @Column(name = "canceled_amount", nullable = false)
    private Long canceledAmount;

    @Column(name = "recipient_name", nullable = false, length = 50)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @Column(name = "address_base", nullable = false, length = 255)
    private String addressBase;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "delivery_request", length = 255)
    private String deliveryRequest;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "saga_correlation_id")
    private UUID sagaCorrelationId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static Order create(
            String orderNo,
            UUID userId,
            OrderType orderType,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String addressBase,
            String addressDetail,
            String deliveryRequest,
            long totalProductAmount,
            long totalDeliveryFee,
            String idempotencyKey
    ) {
        return new Order(
                orderNo,
                userId,
                orderType,
                recipientName,
                recipientPhone,
                zipCode,
                addressBase,
                addressDetail,
                deliveryRequest,
                totalProductAmount,
                totalDeliveryFee,
                idempotencyKey
        );
    }

    private Order(
            String orderNo,
            UUID userId,
            OrderType orderType,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String addressBase,
            String addressDetail,
            String deliveryRequest,
            long totalProductAmount,
            long totalDeliveryFee,
            String idempotencyKey
    ) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("주문번호는 필수입니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("주문자 ID는 필수입니다.");
        }
        if (orderType == null) {
            throw new IllegalArgumentException("주문 유형은 필수입니다.");
        }
        if (recipientName == null || recipientName.isBlank()) {
            throw new IllegalArgumentException("수령인명은 필수입니다.");
        }
        if (recipientPhone == null || recipientPhone.isBlank()) {
            throw new IllegalArgumentException("수령인 연락처는 필수입니다.");
        }
        if (zipCode == null || zipCode.isBlank()) {
            throw new IllegalArgumentException("우편번호는 필수입니다.");
        }
        if (addressBase == null || addressBase.isBlank()) {
            throw new IllegalArgumentException("기본주소는 필수입니다.");
        }
        if (totalProductAmount < 0) {
            throw new IllegalArgumentException("총상품금액은 0 이상이어야 합니다.");
        }
        if (totalDeliveryFee < 0) {
            throw new IllegalArgumentException("총배송비는 0 이상이어야 합니다.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등키는 필수입니다.");
        }

        this.orderNo = orderNo;
        this.userId = userId;
        this.orderType = orderType;
        this.orderStatus = OrderStatus.CREATED;
        this.totalProductAmount = totalProductAmount;
        this.totalDeliveryFee = totalDeliveryFee;
        this.totalPaymentAmount = totalProductAmount + totalDeliveryFee;
        this.canceledAmount = 0L;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.addressBase = addressBase;
        this.addressDetail = addressDetail;
        this.deliveryRequest = deliveryRequest;
        this.orderedAt = Instant.now();
        this.idempotencyKey = idempotencyKey;
    }

    public void markStockReserved(Instant expiresAt) {
        if (orderStatus != OrderStatus.CREATED) {
            throw new IllegalStateException("생성 상태에서만 재고 예약으로 전이할 수 있습니다.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("만료 일시는 필수입니다.");
        }

        this.expiresAt = expiresAt;
        this.orderStatus = OrderStatus.STOCK_RESERVED;
    }

    public void markPaymentPending() {
        if (orderStatus != OrderStatus.STOCK_RESERVED) {
            throw new IllegalStateException("재고 예약 상태에서만 결제 대기로 전이할 수 있습니다.");
        }

        this.orderStatus = OrderStatus.PAYMENT_PENDING;
    }

    public void markPaid(Instant paidAt) {
        if (orderStatus != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 대기 상태에서만 결제 완료로 전이할 수 있습니다.");
        }
        if (paidAt == null) {
            throw new IllegalArgumentException("결제 완료 시각은 필수입니다.");
        }

        this.paidAt = paidAt;
        this.expiresAt = null;
        this.orderStatus = OrderStatus.PAID;
    }

    public void revertToStockReserved() {
        if (orderStatus != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 대기 상태에서만 재고 예약으로 되돌릴 수 있습니다.");
        }

        this.orderStatus = OrderStatus.STOCK_RESERVED;
    }

    public void cancel() {
        if (orderStatus != OrderStatus.CREATED
                && orderStatus != OrderStatus.STOCK_RESERVED
                && orderStatus != OrderStatus.PAYMENT_PENDING
                && orderStatus != OrderStatus.PAID) {
            throw new IllegalStateException("취소할 수 없는 주문 상태입니다.");
        }

        this.orderStatus = OrderStatus.CANCELED;
    }

    public void confirm() {
        if (orderStatus != OrderStatus.PAID) {
            throw new IllegalStateException("결제 완료 상태에서만 구매확정으로 전이할 수 있습니다.");
        }

        this.orderStatus = OrderStatus.CONFIRMED;
    }
}
