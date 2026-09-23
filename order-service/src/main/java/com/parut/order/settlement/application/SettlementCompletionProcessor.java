package com.parut.order.settlement.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;
import com.parut.order.settlement.infrastructure.persistence.SettlementRepository;

import lombok.RequiredArgsConstructor;

/**
 * 정산 한 건을 독립된 트랜잭션으로 조회, 검증하고 완료한다.
 *
 * <p>정산 한 건을 커밋 단위로 보기 위해 REQUIRES_NEW를 사용하며, 다른 정산의 실패가 이미 완료된 정산을 되돌리지 않는다.
 * 새 트랜잭션은 프록시를 거칠 때만 적용되므로 반드시 별도 Bean으로 호출한다.
 */
@Service
@RequiredArgsConstructor
public class SettlementCompletionProcessor {

    private final SettlementRepository settlementRepository;

    /**
     * 정산 한 건을 완료하고 이 메서드의 트랜잭션에서 커밋한다.
     *
     * <p>항목 실패는 호출자가 결과로 변환할 수 있도록 {@link BusinessException}으로 던지고 여기서 삼키지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Settlement completeOne(UUID settlementId, UUID processedBy, Instant completionTime) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_COMPLETED);
        }
        if (completionTime.isBefore(settlement.getEligibleAt())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        // 변경 감지로 반영되므로 별도 save는 호출하지 않는다.
        settlement.complete(completionTime, processedBy);
        return settlement;
    }
}
