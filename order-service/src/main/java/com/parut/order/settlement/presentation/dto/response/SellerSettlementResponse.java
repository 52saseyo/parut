package com.parut.order.settlement.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;

public record SellerSettlementResponse(
        UUID settlementId,
        UUID orderItemId,
        long salesAmount,
        long settlementAmount,
        SettlementStatus status,
        Instant eligibleAt,
        Instant settledAt
) {
    public static SellerSettlementResponse from(Settlement settlement) {
        return new SellerSettlementResponse(
                settlement.getId(), settlement.getOrderItemId(),
                settlement.getSalesAmount(), settlement.getSettlementAmount(), settlement.getStatus(),
                settlement.getEligibleAt(), settlement.getSettledAt());
    }
}
