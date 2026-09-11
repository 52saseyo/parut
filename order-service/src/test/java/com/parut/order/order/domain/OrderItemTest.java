package com.parut.order.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    private static final Instant CONFIRMED_AT = Instant.parse("2026-09-09T01:00:00Z");

    @Test
    @DisplayName("주문상품을 구매 확정하면 상태와 확정 시각을 기록한다")
    void 구매확정() {
        OrderItem orderItem = orderItem();

        orderItem.confirm(CONFIRMED_AT);

        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.CONFIRMED);
        assertThat(orderItem.getConfirmedAt()).isEqualTo(CONFIRMED_AT);
    }

    @Test
    @DisplayName("주문 상태가 아닌 상품은 구매 확정할 수 없다")
    void 구매확정_상태_검증() {
        OrderItem orderItem = orderItem();
        orderItem.cancel(UUID.randomUUID());

        assertThatThrownBy(() -> orderItem.confirm(CONFIRMED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("주문 상태에서만 구매확정으로 전이할 수 있습니다.");
    }

    private OrderItem orderItem() {
        return OrderItem.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "신고배 5kg 특품",
                "NORMAL",
                "국내산(전남 나주)",
                LocalDate.of(2026, 8, 20),
                "KG",
                BigDecimal.valueOf(5),
                15_000L,
                15_000L,
                1
        );
    }
}
