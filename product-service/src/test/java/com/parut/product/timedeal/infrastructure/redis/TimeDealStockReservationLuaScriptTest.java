package com.parut.product.timedeal.infrastructure.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDealStockReservationLuaScriptTest {

    @Test
    void 선점_결과코드를_정의한다() {
        assertThat(TimeDealStockReservationLuaScript.SOLD_OUT).isZero();
        assertThat(TimeDealStockReservationLuaScript.RESERVED).isEqualTo(1);
        assertThat(TimeDealStockReservationLuaScript.DUPLICATE_ORDER).isEqualTo(2);
        assertThat(TimeDealStockReservationLuaScript.INVALID_QUANTITY).isEqualTo(3);
        assertThat(TimeDealStockReservationLuaScript.STOCK_NOT_INITIALIZED).isEqualTo(4);
        assertThat(TimeDealStockReservationLuaScript.INSUFFICIENT_STOCK).isEqualTo(5);
    }

    @Test
    void 재고확인_차감_선점키저장을_하나의_script에_포함한다() {
        String script = TimeDealStockReservationLuaScript.SCRIPT;

        assertThat(script).contains("KEYS[1]");
        assertThat(script).contains("KEYS[2]");
        assertThat(script).contains("EXISTS");
        assertThat(script).contains("DECRBY");
        assertThat(script).contains("SET");
        assertThat(script).contains("EX");
        assertThat(script).contains("return 0");
        assertThat(script).contains("return 1");
        assertThat(script).contains("return 2");
        assertThat(script).contains("return 3");
        assertThat(script).contains("return 4");
        assertThat(script).contains("return 5");
    }
}
