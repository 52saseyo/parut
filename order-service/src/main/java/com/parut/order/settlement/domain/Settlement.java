package com.parut.order.settlement.domain;

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
@Table(name = "p_settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends UpdatableEntity {

    @Column(name = "delivery_group_id", nullable = false)
    private UUID deliveryGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "sales_amount", nullable = false)
    private Long salesAmount;

    @Column(name = "settlement_amount", nullable = false)
    private Long settlementAmount;

    @Column(name = "eligible_at", nullable = false)
    private OffsetDateTime eligibleAt;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static Settlement create(
            UUID deliveryGroupId,
            long salesAmount,
            long settlementAmount,
            OffsetDateTime eligibleAt
    ) {
        return new Settlement(deliveryGroupId, salesAmount, settlementAmount, eligibleAt);
    }

    private Settlement(
            UUID deliveryGroupId,
            long salesAmount,
            long settlementAmount,
            OffsetDateTime eligibleAt
    ) {
        if (deliveryGroupId == null) {
            throw new IllegalArgumentException("배송 그룹 ID는 필수입니다.");
        }
        if (salesAmount < 0) {
            throw new IllegalArgumentException("판매 금액은 0 이상이어야 합니다.");
        }
        if (settlementAmount < 0) {
            throw new IllegalArgumentException("정산 금액은 0 이상이어야 합니다.");
        }
        if (eligibleAt == null) {
            throw new IllegalArgumentException("정산 가능 시각은 필수입니다.");
        }

        this.deliveryGroupId = deliveryGroupId;
        this.salesAmount = salesAmount;
        this.settlementAmount = settlementAmount;
        this.eligibleAt = eligibleAt;
        this.status = SettlementStatus.PENDING;
    }

    public void complete(OffsetDateTime settledAt, UUID processedBy) {
        if (status != SettlementStatus.PENDING) {
            throw new IllegalStateException("정산 대기 상태에서만 정산을 완료할 수 있습니다.");
        }
        if (settledAt == null) {
            throw new IllegalArgumentException("정산 완료 시각은 필수입니다.");
        }
        if (settledAt.isBefore(eligibleAt)) {
            throw new IllegalArgumentException("정산 완료 시각은 정산 가능 시각보다 빠를 수 없습니다.");
        }
        if (processedBy == null) {
            throw new IllegalArgumentException("정산 처리자 ID는 필수입니다.");
        }

        this.settledAt = settledAt;
        this.processedBy = processedBy;
        this.status = SettlementStatus.COMPLETED;
    }
}
