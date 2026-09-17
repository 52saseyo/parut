package com.parut.order.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final long PRODUCT_AMOUNT = 30_000L;
    private static final long DELIVERY_FEE = 3_000L;

    @Test
    @DisplayName("일부 아이템만 취소하면 취소금액만 증가한다")
    void 부분_취소() {
        Order order = paidOrder();

        order.applyCancellation(15_000L);

        assertThat(order.getCanceledAmount()).isEqualTo(15_000L);
    }

    @Test
    @DisplayName("모든 아이템이 취소돼도 주문 상태는 PAID를 유지한다")
    void 전체_취소해도_주문상태_유지() {
        Order order = paidOrder();

        order.applyCancellation(15_000L);
        order.applyCancellation(18_000L);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getCanceledAmount()).isEqualTo(PRODUCT_AMOUNT + DELIVERY_FEE);
    }

    @Test
    @DisplayName("취소 금액 합계가 총결제금액을 넘으면 취소할 수 없다")
    void 취소금액_한도_검증() {
        Order order = paidOrder();
        order.applyCancellation(30_000L);

        assertThatThrownBy(() -> order.applyCancellation(4_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("취소 금액 합계는 총결제금액을 넘을 수 없습니다.");
    }

    private Order paidOrder() {
        Order order = Order.create(
                "20260916-000001",
                UUID.randomUUID(),
                OrderType.NORMAL,
                "김파릇",
                "010-1234-5678",
                "12345",
                "전남 나주시 배꽃로 1",
                "101동 1001호",
                null,
                PRODUCT_AMOUNT,
                DELIVERY_FEE,
                UUID.randomUUID().toString()
        );
        order.markStockReserved(Instant.parse("2026-09-17T01:00:00Z"));
        order.markPaymentPending();
        order.markPaid(Instant.parse("2026-09-16T01:00:00Z"));

        return order;
    }
}
