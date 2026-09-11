package com.parut.product.timedeal.application.dto.timedealpurchase;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

import java.util.UUID;


public record TimeDealPurchaseReserveCommand(
        UUID timeDealId,
        UUID orderId,
        UUID userId,
        Integer quantity
) {
    public TimeDealPurchaseReserveCommand {
        if (timeDealId == null || orderId == null || userId == null || quantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}