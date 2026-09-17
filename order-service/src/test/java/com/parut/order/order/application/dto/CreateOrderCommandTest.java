package com.parut.order.order.application.dto;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateOrderCommandTest {

    private CreateOrderCommand validCommand(List<OrderItemCommand> items) {
        return new CreateOrderCommand(
                UUID.randomUUID(), "idem-key", items,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", null, null
        );
    }

    @Test
    @DisplayName("유효한 값이면 정상 생성된다")
    void 정상_생성() {
        validCommand(List.of(new OrderItemCommand(UUID.randomUUID(), 1)));
    }

    @Test
    @DisplayName("아이템이 비어 있으면 예외를 던진다")
    void 아이템_없음() {
        assertThatThrownBy(() -> validCommand(List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("같은 상품이 중복으로 들어오면 예외를 던진다")
    void 중복_상품ID() {
        UUID productId = UUID.randomUUID();
        assertThatThrownBy(() -> validCommand(List.of(
                new OrderItemCommand(productId, 1),
                new OrderItemCommand(productId, 2)
        )))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("멱등키가 비어 있으면 예외를 던진다")
    void 멱등키_공백() {
        assertThatThrownBy(() -> new CreateOrderCommand(
                UUID.randomUUID(), "  ", List.of(new OrderItemCommand(UUID.randomUUID(), 1)),
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", null, null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
