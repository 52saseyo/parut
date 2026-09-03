package com.parut.order.refund.domain;

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
@Table(name = "p_refunds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends UpdatableEntity {

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "refund_amount", nullable = false)
    private Long refundAmount;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "canceled_at")
    private OffsetDateTime canceledAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static Refund request(
            UUID orderItemId,
            long refundAmount,
            String reason,
            OffsetDateTime requestedAt
    ) {
        if (orderItemId == null) {
            throw new IllegalArgumentException("주문 상품 ID는 필수입니다.");
        }
        if (refundAmount < 0) {
            throw new IllegalArgumentException("환불 금액은 0 이상이어야 합니다.");
        }
        validateReason(reason, "환불 사유");
        if (requestedAt == null) {
            throw new IllegalArgumentException("환불 요청 시각은 필수입니다.");
        }

        Refund refund = new Refund();
        refund.orderItemId = orderItemId;
        refund.refundAmount = refundAmount;
        refund.reason = reason;
        refund.requestedAt = requestedAt;
        refund.status = RefundStatus.REQUESTED;
        return refund;
    }

    public void cancel(OffsetDateTime canceledAt) {
        validateRequested();
        if (canceledAt == null) {
            throw new IllegalArgumentException("환불 취소 시각은 필수입니다.");
        }
        validateNotBeforeRequestedAt(canceledAt, "환불 취소 시각");

        this.canceledAt = canceledAt;
        this.status = RefundStatus.CANCELED;
    }

    public void approve(OffsetDateTime processedAt, UUID processedBy) {
        validateRequested();
        validateProcessing(processedAt, processedBy);
        validateNotBeforeRequestedAt(processedAt, "환불 처리 시각");

        this.processedAt = processedAt;
        this.processedBy = processedBy;
        this.status = RefundStatus.APPROVED;
    }

    public void reject(String rejectionReason, OffsetDateTime processedAt, UUID processedBy) {
        validateRequested();
        validateReason(rejectionReason, "환불 거절 사유");
        validateProcessing(processedAt, processedBy);
        validateNotBeforeRequestedAt(processedAt, "환불 처리 시각");

        this.rejectionReason = rejectionReason;
        this.processedAt = processedAt;
        this.processedBy = processedBy;
        this.status = RefundStatus.REJECTED;
    }

    private void validateRequested() {
        if (status != RefundStatus.REQUESTED) {
            throw new IllegalStateException("환불 요청 상태에서만 처리할 수 있습니다.");
        }
    }

    private static void validateReason(String reason, String fieldName) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
        if (reason.length() > 500) {
            throw new IllegalArgumentException(fieldName + "는 500자를 초과할 수 없습니다.");
        }
    }

    private static void validateProcessing(OffsetDateTime processedAt, UUID processedBy) {
        if (processedAt == null) {
            throw new IllegalArgumentException("환불 처리 시각은 필수입니다.");
        }
        if (processedBy == null) {
            throw new IllegalArgumentException("환불 처리자 ID는 필수입니다.");
        }
    }

    private void validateNotBeforeRequestedAt(OffsetDateTime targetAt, String fieldName) {
        if (targetAt.isBefore(requestedAt)) {
            throw new IllegalArgumentException(fieldName + "은 환불 요청 시각보다 빠를 수 없습니다.");
        }
    }
}
