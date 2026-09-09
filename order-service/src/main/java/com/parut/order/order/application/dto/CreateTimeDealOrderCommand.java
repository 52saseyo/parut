package com.parut.order.order.application.dto;

import java.util.UUID;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;

public record CreateTimeDealOrderCommand(
        UUID userId,
        String idempotencyKey,
        UUID timeDealId,
        UUID productId,
        int quantity,
        String recipientName,
        String recipientPhone,
        String zipCode,
        String addressBase,
        String addressDetail,
        String deliveryRequest
) {
    public CreateTimeDealOrderCommand {
        if (userId == null
                || idempotencyKey == null || idempotencyKey.isBlank()
                || timeDealId == null
                || productId == null
                || recipientName == null || recipientName.isBlank()
                || recipientPhone == null || recipientPhone.isBlank()
                || zipCode == null || zipCode.isBlank()
                || addressBase == null || addressBase.isBlank()
                || quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
