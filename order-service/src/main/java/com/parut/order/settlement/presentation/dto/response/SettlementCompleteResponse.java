package com.parut.order.settlement.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;

public record SettlementCompleteResponse(
        UUID settlementId,
        UUID orderItemId,
        SettlementStatus status,
        Instant settledAt
) {
    public static SettlementCompleteResponse from(Settlement settlement) {
        return new SettlementCompleteResponse(
                settlement.getId(), settlement.getOrderItemId(), settlement.getStatus(), settlement.getSettledAt());
    }
}
