package com.parut.order.settlement.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;

public record AdminSettlementResponse(
        UUID settlementId,
        UUID sellerId,
        UUID orderItemId,
        long salesAmount,
        long settlementAmount,
        SettlementStatus status,
        Instant eligibleAt,
        Instant settledAt,
        UUID processedBy,
        Instant createdAt
) {
    public static AdminSettlementResponse from(Settlement settlement) {
        return new AdminSettlementResponse(
                settlement.getId(), settlement.getSellerId(), settlement.getOrderItemId(),
                settlement.getSalesAmount(), settlement.getSettlementAmount(), settlement.getStatus(),
                settlement.getEligibleAt(), settlement.getSettledAt(), settlement.getProcessedBy(),
                settlement.getCreatedAt());
    }
}
