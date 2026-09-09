package com.parut.order.order.application.dto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;

class CreateOrderCommandTest {

    private CreateOrderCommand validCommand() {
        return new CreateOrderCommand(
                UUID.randomUUID(), "idem-key", UUID.randomUUID(), 1,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", null, null
        );
    }

    @Test
    @DisplayName("유효한 값이면 정상 생성된다")
    void 정상_생성() {
        validCommand();
    }

    @Test
    @DisplayName("수량이 1보다 작으면 예외를 던진다")
    void 수량_1미만() {
        assertThatThrownBy(() -> new CreateOrderCommand(
                UUID.randomUUID(), "idem-key", UUID.randomUUID(), 0,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", null, null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("멱등키가 비어 있으면 예외를 던진다")
    void 멱등키_공백() {
        assertThatThrownBy(() -> new CreateOrderCommand(
                UUID.randomUUID(), "  ", UUID.randomUUID(), 1,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", null, null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("상품 ID가 없으면 예외를 던진다")
    void 상품ID_누락() {
        assertThatThrownBy(() -> new CreateOrderCommand(
                UUID.randomUUID(), "idem-key", null, 1,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", null, null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
