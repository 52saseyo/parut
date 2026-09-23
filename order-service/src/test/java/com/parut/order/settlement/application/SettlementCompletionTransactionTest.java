package com.parut.order.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.config.JpaAuditingConfig;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderItemQueryUseCase;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;
import com.parut.order.settlement.infrastructure.persistence.SettlementRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import({
        SettlementService.class,
        SettlementCompletionProcessor.class,
        JpaAuditingConfig.class
})
// 정산 한 건씩의 커밋 경계를 확인하므로 테스트 전체를 트랜잭션으로 감싸지 않는다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SettlementCompletionTransactionTest {

    private static final UUID ADMIN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SELLER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant ELIGIBLE_AT = Instant.parse("2026-09-19T00:00:00Z");
    private static final Instant COMPLETION_TIME = Instant.parse("2026-09-20T00:00:00Z");
    private static final Instant ALREADY_SETTLED_AT = Instant.parse("2026-09-19T12:00:00Z");

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private SettlementRepository settlementRepository;

    @MockitoBean
    private OrderItemQueryUseCase orderItemQueryUseCase;

    @Test
    @DisplayName("항목 실패 앞뒤의 정산을 각각 독립된 트랜잭션으로 커밋한다")
    void 항목_실패_앞뒤_정산_독립_커밋() {
        Settlement first = settlementRepository.save(pendingSettlement());
        Settlement completed = settlementRepository.save(completedSettlement());
        Settlement third = settlementRepository.save(pendingSettlement());

        List<SettlementCompletionResult> results = settlementService.completeSettlements(
                List.of(first.getId(), completed.getId(), third.getId()), ADMIN_ID, COMPLETION_TIME);

        // 결과는 요청 순서를 유지하고 실패 항목도 같은 목록에 담긴다.
        assertThat(results).extracting(SettlementCompletionResult::settlementId)
                .containsExactly(first.getId(), completed.getId(), third.getId());
        assertThat(results).extracting(SettlementCompletionResult::isSuccess)
                .containsExactly(true, false, true);
        assertThat(results.get(1).errorCode()).isEqualTo(ErrorCode.SETTLEMENT_ALREADY_COMPLETED);

        // 조율 트랜잭션이 없으므로 아래 조회는 커밋된 상태를 다시 읽는다.
        Settlement reloadedFirst = settlementRepository.findById(first.getId()).orElseThrow();
        assertThat(reloadedFirst.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(reloadedFirst.getSettledAt()).isEqualTo(COMPLETION_TIME);
        assertThat(reloadedFirst.getProcessedBy()).isEqualTo(ADMIN_ID);

        Settlement reloadedThird = settlementRepository.findById(third.getId()).orElseThrow();
        assertThat(reloadedThird.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(reloadedThird.getSettledAt()).isEqualTo(COMPLETION_TIME);
        assertThat(reloadedThird.getProcessedBy()).isEqualTo(ADMIN_ID);

        Settlement reloadedCompleted = settlementRepository.findById(completed.getId()).orElseThrow();
        assertThat(reloadedCompleted.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(reloadedCompleted.getSettledAt()).isEqualTo(ALREADY_SETTLED_AT);
        assertThat(reloadedCompleted.getProcessedBy()).isEqualTo(ADMIN_ID);
    }

    private Settlement pendingSettlement() {
        return Settlement.create(UUID.randomUUID(), SELLER_ID, 10_000L, 10_000L, ELIGIBLE_AT);
    }

    private Settlement completedSettlement() {
        Settlement settlement = pendingSettlement();
        settlement.complete(ALREADY_SETTLED_AT, ADMIN_ID);
        return settlement;
    }
}
