package com.parut.product.timedeal.application.dto.timedealpurchase;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

import java.util.UUID;


public record TimeDealPurchaseCancelCommand(
        UUID orderId,
        String reason
) {
    public TimeDealPurchaseCancelCommand {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}