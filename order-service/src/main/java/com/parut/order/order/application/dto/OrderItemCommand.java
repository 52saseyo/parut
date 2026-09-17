package com.parut.order.order.application.dto;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;

import java.util.UUID;

public record OrderItemCommand(
        UUID productId,
        int quantity
) {
    public OrderItemCommand {
        if (productId == null || quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
