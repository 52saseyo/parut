package com.parut.order.payment.domain;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.global.common.entity.UpdatableEntity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_payment_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction extends UpdatableEntity {

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "pg_transaction_key", length = 200)
    private String pgTransactionKey;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "balance_amount_after")
    private Long balanceAmountAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false, length = 20)
    private TransactionStatus transactionStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static PaymentTransaction create(
            UUID paymentId,
            TransactionType transactionType,
            long amount,
            String idempotencyKey,
            String requestPayload
    ) {
        return new PaymentTransaction(paymentId, transactionType, amount, idempotencyKey, requestPayload);
    }

    private PaymentTransaction(
            UUID paymentId,
            TransactionType transactionType,
            long amount,
            String idempotencyKey,
            String requestPayload
    ) {
        if (paymentId == null) {
            throw new IllegalArgumentException("결제 ID는 필수입니다.");
        }
        if (transactionType == null) {
            throw new IllegalArgumentException("거래 유형은 필수입니다.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("거래금액은 0 이상이어야 합니다.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등키는 필수입니다.");
        }

        this.paymentId = paymentId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.requestPayload = requestPayload;
        // PG 호출 전 FAILED로 먼저 기록한 뒤 응답을 받으면 complete()로 갱신
        this.transactionStatus = TransactionStatus.FAILED;
        this.requestedAt = Instant.now();
    }

    public void complete(
            TransactionStatus transactionStatus,
            String pgTransactionKey,
            Long balanceAmountAfter,
            String reason,
            String responsePayload,
            Instant completedAt
    ) {
        if (this.transactionStatus != TransactionStatus.FAILED || this.completedAt != null) {
            throw new IllegalStateException("완료 처리되지 않은 거래에서만 결과를 반영할 수 있습니다.");
        }
        if (transactionStatus == null) {
            throw new IllegalArgumentException("거래 상태는 필수입니다.");
        }
        if (completedAt == null) {
            throw new IllegalArgumentException("완료 시각은 필수입니다.");
        }

        this.transactionStatus = transactionStatus;
        this.pgTransactionKey = pgTransactionKey;
        this.balanceAmountAfter = balanceAmountAfter;
        this.reason = reason;
        this.responsePayload = responsePayload;
        this.completedAt = completedAt;
    }
}
