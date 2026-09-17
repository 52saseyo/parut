package com.parut.order.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderCancelTest {

    @Test
    @DisplayName("취소 사유 코드가 취소 주체 유형과 맞지 않으면 취소 이력을 만들 수 없다")
    void 사유코드_주체_정합성() {
        assertThatThrownBy(() -> orderCancel(CancelReasonCode.CUSTOMER_CANCEL, CanceledByType.SELLER, "사유"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("취소 사유 코드와 취소 주체 유형이 맞지 않습니다.");
    }

    @Test
    @DisplayName("판매자 취소는 취소 사유가 없으면 취소 이력을 만들 수 없다")
    void 판매자_사유_필수() {
        assertThatThrownBy(() -> orderCancel(CancelReasonCode.SELLER_CANCEL, CanceledByType.SELLER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("판매자 취소는 취소 사유가 필수입니다.");
    }

    @Test
    @DisplayName("취소 총액은 상품금액과 배송비의 합으로 계산된다")
    void 취소_총액_계산() {
        OrderCancel orderCancel = orderCancel(CancelReasonCode.CUSTOMER_CANCEL, CanceledByType.CUSTOMER, null);

        assertThat(orderCancel.getCancelTotalAmount()).isEqualTo(33_000L);
    }

    private OrderCancel orderCancel(
            CancelReasonCode cancelReasonCode,
            CanceledByType canceledByType,
            String cancelReason
    ) {
        return OrderCancel.create(
                UUID.randomUUID(),
                cancelReasonCode,
                cancelReason,
                canceledByType,
                UUID.randomUUID().toString(),
                30_000L,
                3_000L,
                true,
                "idem-key-0001"
        );
    }
}
