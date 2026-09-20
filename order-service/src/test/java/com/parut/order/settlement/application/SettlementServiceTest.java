package com.parut.order.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderItemQueryUseCase;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;
import com.parut.order.settlement.infrastructure.persistence.SettlementRepository;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID FIRST_SETTLEMENT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");
    private static final UUID SECOND_SETTLEMENT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b4");
    private static final UUID FIRST_ORDER_ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b5");
    private static final UUID SECOND_ORDER_ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b6");
    private static final Instant COMPLETION_TIME = Instant.parse("2026-09-20T00:00:00Z");
    private static final Instant ELIGIBLE_AT = Instant.parse("2026-09-19T00:00:00Z");

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private OrderItemQueryUseCase orderItemQueryUseCase;

    @InjectMocks
    private SettlementService settlementService;

    @Test
    @DisplayName("여러 PENDING 정산을 같은 시각과 관리자로 완료한다")
    void 다건_완료() {
        Settlement first = settlement(FIRST_ORDER_ITEM_ID, ELIGIBLE_AT);
        Settlement second = settlement(SECOND_ORDER_ITEM_ID, ELIGIBLE_AT);
        when(settlementRepository.findAllById(List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID)))
                .thenReturn(List.of(first, second));

        settlementService.completeSettlements(
                List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID), ADMIN_ID, COMPLETION_TIME);

        assertThat(first.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(first.getSettledAt()).isEqualTo(COMPLETION_TIME);
        assertThat(second.getSettledAt()).isEqualTo(COMPLETION_TIME);
        assertThat(first.getProcessedBy()).isEqualTo(ADMIN_ID);
        assertThat(second.getProcessedBy()).isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("완료된 정산이 섞이면 다른 정산도 변경하지 않는다")
    void 완료된_정산_포함_전체_실패() {
        Settlement pending = settlement(FIRST_ORDER_ITEM_ID, ELIGIBLE_AT);
        Settlement completed = settlement(SECOND_ORDER_ITEM_ID, ELIGIBLE_AT);
        completed.complete(COMPLETION_TIME.minusSeconds(1), ADMIN_ID);
        when(settlementRepository.findAllById(List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID)))
                .thenReturn(List.of(pending, completed));

        assertThatThrownBy(() -> settlementService.completeSettlements(
                List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID), ADMIN_ID, COMPLETION_TIME))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_ALREADY_COMPLETED);
        assertThat(pending.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    @DisplayName("없는 정산이 섞이면 전체를 변경하지 않는다")
    void 없는_정산_포함_전체_실패() {
        Settlement pending = settlement(FIRST_ORDER_ITEM_ID, ELIGIBLE_AT);
        when(settlementRepository.findAllById(List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID)))
                .thenReturn(List.of(pending));

        assertThatThrownBy(() -> settlementService.completeSettlements(
                List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID), ADMIN_ID, COMPLETION_TIME))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
        assertThat(pending.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    @DisplayName("정산 가능 시각 전인 대상이 섞이면 다른 정산도 변경하지 않는다")
    void 정산_가능_시각_전_전체_실패() {
        Settlement pending = settlement(FIRST_ORDER_ITEM_ID, ELIGIBLE_AT);
        Settlement notEligible = settlement(SECOND_ORDER_ITEM_ID, COMPLETION_TIME.plusSeconds(1));
        when(settlementRepository.findAllById(List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID)))
                .thenReturn(List.of(pending, notEligible));

        assertThatThrownBy(() -> settlementService.completeSettlements(
                List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID), ADMIN_ID, COMPLETION_TIME))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
        assertThat(pending.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    private Settlement settlement(UUID orderItemId, Instant eligibleAt) {
        return Settlement.create(orderItemId, SELLER_ID, 10_000L, 10_000L, eligibleAt);
    }
}
