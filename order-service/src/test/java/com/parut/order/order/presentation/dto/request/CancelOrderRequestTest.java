package com.parut.order.order.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.CancelOrderCommand;
import com.parut.order.order.domain.CancelReasonCode;
import com.parut.order.order.domain.CanceledByType;

class CancelOrderRequestTest {

    private static final UUID ORDER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID REQUESTER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");
    private static final String IDEMPOTENCY_KEY = "idem-key-0001";

    @Test
    @DisplayName("구매자 요청은 CUSTOMER 주체로 변환한다")
    void 구매자_주체변환() {
        CancelOrderRequest request = request(CancelReasonCode.CUSTOMER_CANCEL, null);

        CancelOrderCommand command = request.toCommand(ORDER_ID, userContext(UserRole.CUSTOMER), IDEMPOTENCY_KEY);

        assertThat(command.canceledByType()).isEqualTo(CanceledByType.CUSTOMER);
        assertThat(command.cancelReasonCode()).isEqualTo(CancelReasonCode.CUSTOMER_CANCEL);
        assertThat(command.requesterId()).isEqualTo(REQUESTER_ID);
        assertThat(command.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("판매자 요청은 SELLER 주체로 변환한다")
    void 판매자_주체변환() {
        CancelOrderRequest request = request(CancelReasonCode.SELLER_CANCEL, "재고 소진");

        CancelOrderCommand command = request.toCommand(ORDER_ID, userContext(UserRole.SELLER), IDEMPOTENCY_KEY);

        assertThat(command.canceledByType()).isEqualTo(CanceledByType.SELLER);
        assertThat(command.cancelReason()).isEqualTo("재고 소진");
    }

    @Test
    @DisplayName("판매자 요청에 취소 사유가 없으면 CANCEL_REASON_REQUIRED를 던진다")
    void 판매자_사유누락() {
        CancelOrderRequest request = request(CancelReasonCode.SELLER_CANCEL, " ");

        assertThatThrownBy(() -> request.toCommand(ORDER_ID, userContext(UserRole.SELLER), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANCEL_REASON_REQUIRED);
    }

    @Test
    @DisplayName("역할과 맞지 않는 사유 코드는 거부한다")
    void 역할불일치_사유코드() {
        CancelOrderRequest request = request(CancelReasonCode.SELLER_CANCEL, "사유");

        assertThatThrownBy(() -> request.toCommand(ORDER_ID, userContext(UserRole.CUSTOMER), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    private CancelOrderRequest request(CancelReasonCode cancelReasonCode, String cancelReason) {
        return new CancelOrderRequest(List.of(ITEM_ID), cancelReasonCode, cancelReason);
    }

    private UserContext userContext(UserRole role) {
        return UserContext.of(REQUESTER_ID, role);
    }
}
