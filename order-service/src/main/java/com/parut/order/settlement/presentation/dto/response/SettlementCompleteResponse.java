package com.parut.order.settlement.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.parut.order.settlement.application.SettlementCompletionResult;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;

/**
 * 정산 다건 완료의 항목별 결과다.
 *
 * <p>완료하지 못한 정산도 같은 형태로 담고 {@link Result}로 구분한다. 실패 항목은 완료 정보가 없으므로
 * 정산 ID를 제외한 값이 비어 있다.
 */
public record SettlementCompleteResponse(
        UUID settlementId,
        UUID orderItemId,
        SettlementStatus status,
        Instant settledAt,
        Result result
) {
    public enum Result {
        SUCCESS,
        FAILED
    }

    public static SettlementCompleteResponse from(SettlementCompletionResult result) {
        return result.isSuccess()
                ? from(result.settlement())
                : new SettlementCompleteResponse(result.settlementId(), null, null, null, Result.FAILED);
    }

    public static SettlementCompleteResponse from(Settlement settlement) {
        return new SettlementCompleteResponse(
                settlement.getId(), settlement.getOrderItemId(), settlement.getStatus(), settlement.getSettledAt(),
                Result.SUCCESS);
    }
}
