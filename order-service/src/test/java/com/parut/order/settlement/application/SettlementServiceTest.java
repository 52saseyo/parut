package com.parut.order.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderItemQueryUseCase;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.infrastructure.persistence.SettlementRepository;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID FIRST_SETTLEMENT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");
    private static final UUID SECOND_SETTLEMENT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b4");
    private static final UUID THIRD_SETTLEMENT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b7");
    private static final UUID FIRST_ORDER_ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b5");
    private static final UUID SECOND_ORDER_ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b6");
    private static final Instant COMPLETION_TIME = Instant.parse("2026-09-20T00:00:00Z");
    private static final Instant ELIGIBLE_AT = Instant.parse("2026-09-19T00:00:00Z");

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private OrderItemQueryUseCase orderItemQueryUseCase;

    @Mock
    private SettlementCompletionProcessor settlementCompletionProcessor;

    @InjectMocks
    private SettlementService settlementService;

    @Test
    @DisplayName("항목 실패 뒤의 정산까지 계속 처리하고 성공과 실패를 요청 순서대로 구분한다")
    void 성공_실패_성공_계속_처리() {
        Settlement first = settlement(FIRST_ORDER_ITEM_ID, ELIGIBLE_AT);
        Settlement third = settlement(SECOND_ORDER_ITEM_ID, ELIGIBLE_AT);
        when(settlementCompletionProcessor.completeOne(FIRST_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME))
                .thenReturn(first);
        when(settlementCompletionProcessor.completeOne(SECOND_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME))
                .thenThrow(new BusinessException(ErrorCode.SETTLEMENT_ALREADY_COMPLETED));
        when(settlementCompletionProcessor.completeOne(THIRD_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME))
                .thenReturn(third);

        List<SettlementCompletionResult> results = settlementService.completeSettlements(
                List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID, THIRD_SETTLEMENT_ID), ADMIN_ID, COMPLETION_TIME);

        assertThat(results).containsExactly(
                SettlementCompletionResult.success(first),
                SettlementCompletionResult.failure(SECOND_SETTLEMENT_ID, ErrorCode.SETTLEMENT_ALREADY_COMPLETED),
                SettlementCompletionResult.success(third));
        InOrder order = inOrder(settlementCompletionProcessor);
        order.verify(settlementCompletionProcessor).completeOne(FIRST_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME);
        order.verify(settlementCompletionProcessor).completeOne(SECOND_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME);
        order.verify(settlementCompletionProcessor).completeOne(THIRD_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME);
    }

    @Test
    @DisplayName("낙관적 락 충돌은 CONCURRENT_MODIFICATION 항목 실패로 기록한다")
    void 낙관적_락_충돌_항목_실패() {
        Settlement first = settlement(FIRST_ORDER_ITEM_ID, ELIGIBLE_AT);
        when(settlementCompletionProcessor.completeOne(FIRST_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME))
                .thenReturn(first);
        // 커밋 시점 버전 충돌은 Processor 본문이 아니라 프록시에서 던져진다.
        when(settlementCompletionProcessor.completeOne(SECOND_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME))
                .thenThrow(new OptimisticLockingFailureException("정산 버전 충돌"));

        List<SettlementCompletionResult> results = settlementService.completeSettlements(
                List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID), ADMIN_ID, COMPLETION_TIME);

        assertThat(results).containsExactly(
                SettlementCompletionResult.success(first),
                SettlementCompletionResult.failure(SECOND_SETTLEMENT_ID, ErrorCode.CONCURRENT_MODIFICATION));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 전파하고 남은 정산을 처리하지 않는다")
    void 시스템_예외_전파() {
        Settlement first = settlement(FIRST_ORDER_ITEM_ID, ELIGIBLE_AT);
        when(settlementCompletionProcessor.completeOne(FIRST_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME))
                .thenReturn(first);
        when(settlementCompletionProcessor.completeOne(SECOND_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME))
                .thenThrow(new IllegalStateException("트랜잭션을 시작할 수 없습니다."));

        assertThatThrownBy(() -> settlementService.completeSettlements(
                List.of(FIRST_SETTLEMENT_ID, SECOND_SETTLEMENT_ID, THIRD_SETTLEMENT_ID), ADMIN_ID, COMPLETION_TIME))
                .isInstanceOf(IllegalStateException.class);

        verify(settlementCompletionProcessor, never())
                .completeOne(THIRD_SETTLEMENT_ID, ADMIN_ID, COMPLETION_TIME);
    }

    @Test
    @DisplayName("입력 검증에 실패하면 정산을 한 건도 처리하지 않는다")
    void 입력_검증_실패() {
        List<UUID> tooManyIds = IntStream.range(0, 51)
                .mapToObj(index -> UUID.randomUUID())
                .toList();
        List<List<UUID>> invalidIds = Arrays.asList(
                null,
                List.of(),
                tooManyIds,
                List.of(FIRST_SETTLEMENT_ID, FIRST_SETTLEMENT_ID),
                Arrays.asList(FIRST_SETTLEMENT_ID, null));

        invalidIds.forEach(ids -> assertThatThrownBy(
                () -> settlementService.completeSettlements(ids, ADMIN_ID, COMPLETION_TIME))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        assertThatThrownBy(() -> settlementService.completeSettlements(
                List.of(FIRST_SETTLEMENT_ID), null, COMPLETION_TIME))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> settlementService.completeSettlements(
                List.of(FIRST_SETTLEMENT_ID), ADMIN_ID, null))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(settlementCompletionProcessor, never()).completeOne(any(), any(), any());
    }

    private Settlement settlement(UUID orderItemId, Instant eligibleAt) {
        return Settlement.create(orderItemId, SELLER_ID, 10_000L, 10_000L, eligibleAt);
    }
}
