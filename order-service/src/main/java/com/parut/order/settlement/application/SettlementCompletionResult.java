package com.parut.order.settlement.application;

import java.util.UUID;

import com.parut.order.global.exception.ErrorCode;
import com.parut.order.settlement.domain.Settlement;

/**
 * 정산 한 건의 완료 처리 결과다.
 *
 * <p>성공이면 완료된 {@link Settlement}을, 예상 가능한 항목 실패면 요청 ID와 오류 코드를 담는다.
 * 조율자는 이 결과를 요청 순서대로 모아 반환한다.
 */
public record SettlementCompletionResult(
        UUID settlementId,
        Settlement settlement,
        ErrorCode errorCode
) {
    public static SettlementCompletionResult success(Settlement settlement) {
        return new SettlementCompletionResult(settlement.getId(), settlement, null);
    }

    public static SettlementCompletionResult failure(UUID settlementId, ErrorCode errorCode) {
        return new SettlementCompletionResult(settlementId, null, errorCode);
    }

    public boolean isSuccess() {
        return errorCode == null;
    }
}
