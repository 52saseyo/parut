package com.parut.product.timedeal.infrastructure.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDealScheduleClaimLuaScriptTest {

    @Test
    void 만료된_선점복구와_due_작업_선점을_하나의_script에서_처리한다() {
        String script = TimeDealScheduleClaimLuaScript.CLAIM_SCRIPT;

        assertThat(script).contains("ZRANGEBYSCORE");
        assertThat(script).contains("ZREM");
        assertThat(script).contains("ZADD");
        assertThat(script).contains("KEYS[1]");
        assertThat(script).contains("KEYS[2]");
        assertThat(script).contains("ARGV[1]");
        assertThat(script).contains("ARGV[2]");
        assertThat(script).contains("ARGV[3]");
    }
}
