package com.parut.order.order.application.dto;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
        UUID userId,
        String idempotencyKey,
        List<OrderItemCommand> items,
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
                || items == null || items.isEmpty()
                || recipientName == null || recipientName.isBlank()
                || recipientPhone == null || recipientPhone.isBlank()
                || zipCode == null || zipCode.isBlank()
                || addressBase == null || addressBase.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (items.stream().map(OrderItemCommand::productId).distinct().count() != items.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
