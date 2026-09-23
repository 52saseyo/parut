package com.parut.order.settlement.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.common.OffsetResponse;
import com.parut.order.global.common.PaginationType;
import com.parut.order.global.common.SortDirection;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.settlement.application.SettlementCompletionResult;
import com.parut.order.settlement.application.SettlementPage;
import com.parut.order.settlement.application.SettlementService;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;
import com.parut.order.settlement.presentation.dto.request.CompleteSettlementsRequest;
import com.parut.order.settlement.presentation.dto.response.AdminSettlementResponse;
import com.parut.order.settlement.presentation.dto.response.SettlementCompleteResponse;

@ExtendWith(MockitoExtension.class)
class SettlementControllerTest {

    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID ADMIN_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID FAILED_SETTLEMENT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");

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
    @DisplayName("관리자 목록은 status 기본값 PENDING과 sellerId 필터를 Service에 전달한다")
    void 관리자_목록_조회() {
        UUID sellerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(settlementService.getAdminSettlements(sellerId, SettlementStatus.PENDING, pageable))
                .thenReturn(Page.empty(pageable));

        ApiResponse<OffsetResponse<AdminSettlementResponse>> response =
                settlementController.getAdminSettlements(SettlementStatus.PENDING, sellerId, pageable);

        verify(settlementService).getAdminSettlements(sellerId, SettlementStatus.PENDING, pageable);
        assertThat(response.data().pageInfo().paginationType()).isEqualTo(PaginationType.OFFSET);
        assertThat(response.data().pageInfo().sort()).isEqualTo("createdAt");
        assertThat(response.data().pageInfo().direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    @DisplayName("관리자 목록은 COMPLETED 정산의 settledAt과 processedBy를 응답에 담는다")
    void 관리자_목록_완료_응답_필드() {
        UUID processedBy = UUID.randomUUID();
        Instant settledAt = Instant.parse("2026-09-10T00:00:00Z");
        Settlement settlement = Settlement.create(
                UUID.randomUUID(), SELLER_ID, 10000L, 10000L, Instant.parse("2026-09-01T00:00:00Z"));
        settlement.complete(settledAt, processedBy);
        Pageable pageable = PageRequest.of(0, 10);
        when(settlementService.getAdminSettlements(null, SettlementStatus.COMPLETED, pageable))
                .thenReturn(new PageImpl<>(List.of(settlement), pageable, 1));

        ApiResponse<OffsetResponse<AdminSettlementResponse>> response =
                settlementController.getAdminSettlements(SettlementStatus.COMPLETED, null, pageable);

        AdminSettlementResponse first = response.data().content().get(0);
        assertThat(first.status()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(first.settledAt()).isEqualTo(settledAt);
        assertThat(first.processedBy()).isEqualTo(processedBy);
        assertThat(first.sellerId()).isEqualTo(SELLER_ID);
    }

    @Test
    @DisplayName("판매자 목록의 잘못된 Cursor와 size는 입력 오류로 거부한다")
    void 목록_입력_검증() {
        UserContext seller = UserContext.of(SELLER_ID, UserRole.SELLER);
        assertThatThrownBy(() -> settlementController.getSettlements(
                SettlementStatus.PENDING, "cursor", null, 10, seller))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> settlementController.getSettlements(
                SettlementStatus.PENDING, null, null, 20, seller))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAGE_SIZE);
        assertThatThrownBy(() -> settlementController.getSettlements(
                SettlementStatus.PENDING, "invalid", UUID.randomUUID(), 10, seller))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("다건 완료는 완료하지 못한 정산도 같은 목록에 담아 OK로 응답한다")
    void 다건_완료_항목별_결과_응답() {
        Settlement completed = completedSettlement();
        when(settlementService.completeSettlements(
                eq(List.of(completed.getId(), FAILED_SETTLEMENT_ID)), eq(ADMIN_ID), any(Instant.class)))
                .thenReturn(List.of(
                        SettlementCompletionResult.success(completed),
                        SettlementCompletionResult.failure(
                                FAILED_SETTLEMENT_ID, ErrorCode.SETTLEMENT_ALREADY_COMPLETED)));

        ApiResponse<List<SettlementCompleteResponse>> response = settlementController.completeSettlements(
                new CompleteSettlementsRequest(List.of(completed.getId(), FAILED_SETTLEMENT_ID)),
                UserContext.of(ADMIN_ID, UserRole.ADMIN));

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data()).hasSize(2);

        SettlementCompleteResponse success = response.data().get(0);
        assertThat(success.result()).isEqualTo(SettlementCompleteResponse.Result.SUCCESS);
        assertThat(success.settlementId()).isEqualTo(completed.getId());
        assertThat(success.orderItemId()).isEqualTo(completed.getOrderItemId());
        assertThat(success.status()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(success.settledAt()).isEqualTo(completed.getSettledAt());

        // 완료하지 못한 정산은 요청 ID와 결과만 남기고 완료 정보를 비운다.
        SettlementCompleteResponse failed = response.data().get(1);
        assertThat(failed.result()).isEqualTo(SettlementCompleteResponse.Result.FAILED);
        assertThat(failed.settlementId()).isEqualTo(FAILED_SETTLEMENT_ID);
        assertThat(failed.orderItemId()).isNull();
        assertThat(failed.status()).isNull();
        assertThat(failed.settledAt()).isNull();
    }

    private Settlement completedSettlement() {
        Settlement settlement = Settlement.create(
                UUID.randomUUID(), SELLER_ID, 10_000L, 10_000L, Instant.parse("2026-09-19T00:00:00Z"));
        settlement.complete(Instant.parse("2026-09-20T00:00:00Z"), ADMIN_ID);
        ReflectionTestUtils.setField(settlement, "id", UUID.randomUUID());
        return settlement;
    }
}
