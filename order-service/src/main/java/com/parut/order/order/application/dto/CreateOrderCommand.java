package com.parut.order.order.application.dto;

import java.util.UUID;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;

// ToDo: bulk 도입 시 수정 예정
public record CreateOrderCommand(
        UUID userId,
        String idempotencyKey,
        UUID productId,
        int quantity,
        String recipientName,
        String recipientPhone,
        String zipCode,
        String addressBase,
        String addressDetail,
        String deliveryRequest
) {
    public CreateOrderCommand {
        if (userId == null
                || idempotencyKey == null || idempotencyKey.isBlank()
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
