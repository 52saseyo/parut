package com.parut.order.payment.domain;

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
@Table(name = "p_payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends UpdatableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_no", nullable = false, length = 30)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pg_provider", nullable = false, length = 20)
    private String pgProvider;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "balance_amount", nullable = false)
    private Long balanceAmount;

    @Column(name = "canceled_amount", nullable = false)
    private Long canceledAmount;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "saga_correlation_id")
    private UUID sagaCorrelationId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static Payment create(
            UUID orderId,
            String orderNo,
            UUID userId,
            long totalAmount,
            String idempotencyKey
    ) {
        return new Payment(orderId, orderNo, userId, totalAmount, idempotencyKey);
    }

    private Payment(
            UUID orderId,
            String orderNo,
            UUID userId,
            long totalAmount,
            String idempotencyKey
    ) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("주문번호는 필수입니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("결제자 ID는 필수입니다.");
        }
        if (totalAmount < 0) {
            throw new IllegalArgumentException("총결제금액은 0 이상이어야 합니다.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등키는 필수입니다.");
        }

        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.pgProvider = "TOSS";
        this.paymentStatus = PaymentStatus.READY;
        this.totalAmount = totalAmount;
        this.balanceAmount = totalAmount;
        this.canceledAmount = 0L;
        this.requestedAt = Instant.now();
        this.idempotencyKey = idempotencyKey;
    }

    public void start() {
        if (paymentStatus != PaymentStatus.READY) {
            throw new IllegalStateException("결제 준비 상태에서만 결제창 진입으로 전이할 수 있습니다.");
        }

        this.paymentStatus = PaymentStatus.IN_PROGRESS;
    }

    public void approve(String paymentKey, PaymentMethod paymentMethod, Instant approvedAt, String receiptUrl) {
        if (paymentStatus != PaymentStatus.IN_PROGRESS) {
            throw new IllegalStateException("결제 진행 상태에서만 승인으로 전이할 수 있습니다.");
        }
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalArgumentException("PG 결제키는 필수입니다.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("결제 수단은 필수입니다.");
        }
        if (approvedAt == null) {
            throw new IllegalArgumentException("결제 승인 시각은 필수입니다.");
        }
        if (!balanceAmount.equals(totalAmount)) {
            throw new IllegalStateException("승인 전 잔액은 총결제금액과 같아야 합니다.");
        }

        this.paymentKey = paymentKey;
        this.paymentMethod = paymentMethod;
        this.approvedAt = approvedAt;
        this.receiptUrl = receiptUrl;
        this.paymentStatus = PaymentStatus.DONE;
    }

    public void abort() {
        if (paymentStatus != PaymentStatus.IN_PROGRESS) {
            throw new IllegalStateException("결제 진행 상태에서만 중단으로 전이할 수 있습니다.");
        }

        this.paymentStatus = PaymentStatus.ABORTED;
    }

    public void retry(String newIdempotencyKey) {
        if (paymentStatus != PaymentStatus.ABORTED) {
            throw new IllegalStateException("중단 상태에서만 재시도할 수 있습니다.");
        }
        if (newIdempotencyKey == null || newIdempotencyKey.isBlank()) {
            throw new IllegalArgumentException("재발급 멱등키는 필수입니다.");
        }

        this.idempotencyKey = newIdempotencyKey;
        this.paymentStatus = PaymentStatus.IN_PROGRESS;
    }

    public void applyCancellation(long cancelAmount, Instant canceledAt) {
        if (paymentStatus != PaymentStatus.DONE && paymentStatus != PaymentStatus.PARTIAL_CANCELED) {
            throw new IllegalStateException("결제 완료·부분 취소 상태에서만 취소를 반영할 수 있습니다.");
        }
        if (cancelAmount <= 0 || cancelAmount > balanceAmount) {
            throw new IllegalArgumentException("취소 금액은 0보다 크고 잔액 이하여야 합니다.");
        }

        this.balanceAmount -= cancelAmount;
        this.canceledAmount += cancelAmount;

        if (balanceAmount == 0) {
            this.paymentStatus = PaymentStatus.CANCELED;
            this.canceledAt = canceledAt;
        } else {
            this.paymentStatus = PaymentStatus.PARTIAL_CANCELED;
        }
    }
}
