package com.parut.order.settlement.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.settlement.application.SettlementPage;
import com.parut.order.settlement.application.SettlementService;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;

@ExtendWith(MockitoExtension.class)
class SettlementControllerTest {

    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");

    @Mock
    private SettlementService settlementService;

    @InjectMocks
    private SettlementController settlementController;

    @Test
    @DisplayName("판매자 목록은 Settlement 필드만 응답하고 sellerId와 PENDING을 전달한다")
    void 판매자_목록_조회() {
        Settlement settlement = Settlement.create(
                UUID.randomUUID(), SELLER_ID, 30_000L, 30_000L, Instant.parse("2026-09-01T00:00:00Z"));
        SettlementPage page = new SettlementPage(List.of(settlement), null, null, false);
        when(settlementService.getSellerSettlements(eq(SELLER_ID), eq(SettlementStatus.PENDING), eq(null), eq(null), eq(10)))
                .thenReturn(page);
        var response = settlementController.getSettlements(
                SettlementStatus.PENDING, null, null, 10, UserContext.of(SELLER_ID, UserRole.SELLER));

        assertThat(response.data().content()).hasSize(1);
        assertThat(response.data().content().get(0).orderItemId()).isEqualTo(settlement.getOrderItemId());
        verify(settlementService).getSellerSettlements(SELLER_ID, SettlementStatus.PENDING, null, null, 10);
    }

    @Test
    @DisplayName("판매자 목록은 COMPLETED 필터와 커서를 Service에 전달한다")
    void 판매자_목록_필터_커서() {
        Instant cursor = Instant.parse("2026-09-01T00:00:00Z");
        UUID cursorId = UUID.randomUUID();
        when(settlementService.getSellerSettlements(SELLER_ID, SettlementStatus.COMPLETED, cursor, cursorId, 30))
                .thenReturn(new SettlementPage(List.of(), null, null, false));
        settlementController.getSettlements(SettlementStatus.COMPLETED, "2026-09-01T00:00:00Z", cursorId, 30,
                UserContext.of(SELLER_ID, UserRole.SELLER));

        verify(settlementService).getSellerSettlements(SELLER_ID, SettlementStatus.COMPLETED, cursor, cursorId, 30);
    }

    @Test
    @DisplayName("관리자 대상 목록은 PENDING과 sellerId 필터를 전달한다")
    void 관리자_목록_조회() {
        UUID sellerId = UUID.randomUUID();
        when(settlementService.getAdminSettlements(sellerId, null, null, 10))
                .thenReturn(new SettlementPage(List.of(), null, null, false));

        settlementController.getSettlementTargets(sellerId, null, null, 10);

        verify(settlementService).getAdminSettlements(sellerId, null, null, 10);
    }

    @Test
    @DisplayName("잘못된 Cursor와 size는 입력 오류로 거부한다")
    void 목록_입력_검증() {
        assertThatThrownBy(() -> settlementController.getSettlementTargets(null, "cursor", null, 10))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> settlementController.getSettlementTargets(null, null, null, 20))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAGE_SIZE);
        assertThatThrownBy(() -> settlementController.getSettlementTargets(
                null, "invalid", UUID.randomUUID(), 10))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
