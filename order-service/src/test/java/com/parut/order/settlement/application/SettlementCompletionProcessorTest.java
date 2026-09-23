package com.parut.order.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;
import com.parut.order.settlement.infrastructure.persistence.SettlementRepository;

@ExtendWith(MockitoExtension.class)
class SettlementCompletionProcessorTest {

    private static final UUID ADMIN_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID SETTLEMENT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");
    private static final UUID ORDER_ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b5");
    private static final Instant COMPLETION_TIME = Instant.parse("2026-09-20T00:00:00Z");
    private static final Instant ELIGIBLE_AT = Instant.parse("2026-09-19T00:00:00Z");

    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private SettlementCompletionProcessor settlementCompletionProcessor;

    @Test
    @DisplayName("존재하지 않는 정산은 SETTLEMENT_NOT_FOUND로 거부한다")
    void 없는_정산() {
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementCompletionProcessor.completeOne(SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("정산 가능 시각 전이면 INVALID_STATE_TRANSITION으로 거부한다")
    void 정산_가능_시각_전() {
        Settlement settlement = settlement(COMPLETION_TIME.plusSeconds(1));
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> settlementCompletionProcessor.completeOne(SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    private Settlement settlement(Instant eligibleAt) {
        return Settlement.create(ORDER_ITEM_ID, SELLER_ID, 10_000L, 10_000L, eligibleAt);
    }
}
