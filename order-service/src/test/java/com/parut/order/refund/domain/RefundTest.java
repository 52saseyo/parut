package com.parut.order.refund.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RefundTest {

    private static final UUID ORDER_ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b8");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b9");
    private static final UUID CUSTOMER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6ba");
    private static final Instant REQUESTED_AT = Instant.parse("2026-09-06T01:00:00Z");

    @Nested
    @DisplayName("요청")
    class Request {

        @Test
        @DisplayName("환불 요청 상태로 생성된다")
        void 환불_요청_성공() {
            Refund refund = refund();

            assertThat(refund.getOrderItemId()).isEqualTo(ORDER_ITEM_ID);
            assertThat(refund.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(refund.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(refund.getRefundAmount()).isEqualTo(10_000L);
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        }

        @Test
        @DisplayName("주문상품 ID, 환불금액과 사유를 검증한다")
        void 환불_요청값_검증() {
            assertThatThrownBy(() -> Refund.request(null, CUSTOMER_ID, SELLER_ID, 10_000L, "상품 불량", REQUESTED_AT))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Refund.request(ORDER_ITEM_ID, CUSTOMER_ID, SELLER_ID, -1L, "상품 불량", REQUESTED_AT))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Refund.request(ORDER_ITEM_ID, CUSTOMER_ID, SELLER_ID, 10_000L, " ", REQUESTED_AT))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("처리")
    class Process {

        @Test
        @DisplayName("요청 상태의 환불을 취소한다")
        void 환불_요청_취소() {
            Refund refund = refund();
            Instant canceledAt = REQUESTED_AT.plusSeconds(60);

            refund.cancel(canceledAt);

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.CANCELED);
            assertThat(refund.getCanceledAt()).isEqualTo(canceledAt);
        }

        @Test
        @DisplayName("승인되거나 거절된 환불은 다시 처리할 수 없다")
        void 최종_상태_재처리_금지() {
            Refund approved = refund();
            approved.approve(REQUESTED_AT.plusSeconds(60), SELLER_ID);
            Refund rejected = refund();
            rejected.reject("환불 대상이 아닙니다.", REQUESTED_AT.plusSeconds(60), SELLER_ID);

            assertThatThrownBy(() -> approved.cancel(REQUESTED_AT.plusSeconds(120)))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> rejected.approve(REQUESTED_AT.plusSeconds(120), SELLER_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    private Refund refund() {
        return Refund.request(ORDER_ITEM_ID, CUSTOMER_ID, SELLER_ID, 10_000L, "상품 불량", REQUESTED_AT);
    }
}
