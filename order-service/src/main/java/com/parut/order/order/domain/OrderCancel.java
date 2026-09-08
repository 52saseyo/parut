package com.parut.order.order.domain;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.global.common.entity.UpdatableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_order_cancels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderCancel extends UpdatableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason_code", nullable = false, length = 30)
    private CancelReasonCode cancelReasonCode;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "canceled_by_type", nullable = false, length = 20)
    private CanceledByType canceledByType;

    @Column(name = "canceled_by", nullable = false, length = 50)
    private String canceledBy;

    @Column(name = "cancel_product_amount", nullable = false)
    private Long cancelProductAmount;

    @Column(name = "cancel_delivery_fee", nullable = false)
    private Long cancelDeliveryFee;

    @Column(name = "cancel_total_amount", nullable = false)
    private Long cancelTotalAmount;

    @Column(name = "refund_required", nullable = false)
    private Boolean refundRequired;

    @Column(name = "payment_transaction_id")
    private UUID paymentTransactionId;

    @Column(name = "canceled_at", nullable = false)
    private Instant canceledAt;

    public static OrderCancel create(
            UUID orderId,
            CancelReasonCode cancelReasonCode,
            String cancelReason,
            CanceledByType canceledByType,
            String canceledBy,
            long cancelProductAmount,
            long cancelDeliveryFee,
            boolean refundRequired
    ) {
        return new OrderCancel(
                orderId,
                cancelReasonCode,
                cancelReason,
                canceledByType,
                canceledBy,
                cancelProductAmount,
                cancelDeliveryFee,
                refundRequired
        );
    }

    private OrderCancel(
            UUID orderId,
            CancelReasonCode cancelReasonCode,
            String cancelReason,
            CanceledByType canceledByType,
            String canceledBy,
            long cancelProductAmount,
            long cancelDeliveryFee,
            boolean refundRequired
    ) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (cancelReasonCode == null) {
            throw new IllegalArgumentException("취소 사유 코드는 필수입니다.");
        }
        if (canceledByType == null) {
            throw new IllegalArgumentException("취소 주체 유형은 필수입니다.");
        }
        if (canceledBy == null || canceledBy.isBlank()) {
            throw new IllegalArgumentException("취소 주체는 필수입니다.");
        }
        if (canceledByType == CanceledByType.SELLER && (cancelReason == null || cancelReason.isBlank())) {
            throw new IllegalArgumentException("판매자 취소는 취소 사유가 필수입니다.");
        }
        if (cancelProductAmount < 0) {
            throw new IllegalArgumentException("취소 상품금액은 0 이상이어야 합니다.");
        }
        if (cancelDeliveryFee < 0) {
            throw new IllegalArgumentException("취소 배송비는 0 이상이어야 합니다.");
        }

        this.orderId = orderId;
        this.cancelReasonCode = cancelReasonCode;
        this.cancelReason = cancelReason;
        this.canceledByType = canceledByType;
        this.canceledBy = canceledBy;
        this.cancelProductAmount = cancelProductAmount;
        this.cancelDeliveryFee = cancelDeliveryFee;
        this.cancelTotalAmount = cancelProductAmount + cancelDeliveryFee;
        this.refundRequired = refundRequired;
        this.canceledAt = Instant.now();
    }
}
