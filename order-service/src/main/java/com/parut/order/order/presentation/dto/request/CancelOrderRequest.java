package com.parut.order.order.presentation.dto.request;

import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.CancelOrderCommand;
import com.parut.order.order.domain.CancelReasonCode;
import com.parut.order.order.domain.CanceledByType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CancelOrderRequest(

        @NotEmpty
        List<UUID> orderItemIds,

        @NotNull
        CancelReasonCode cancelReasonCode,

        String cancelReason
) {
    public CancelOrderCommand toCommand(UUID orderId, UserContext userContext, String idempotencyKey) {
        CanceledByType canceledByType = canceledByTypeOf(userContext.role());

        // 시스템 전용 사유 코드나 상대 역할의 코드를 지정하는 요청 방어
        if (!cancelReasonCode.matches(canceledByType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (canceledByType == CanceledByType.SELLER && (cancelReason == null || cancelReason.isBlank())) {
            throw new BusinessException(ErrorCode.CANCEL_REASON_REQUIRED);
        }

        return new CancelOrderCommand(
                orderId,
                orderItemIds,
                cancelReasonCode,
                cancelReason,
                canceledByType,
                userContext.userId(),
                idempotencyKey
        );
    }

    private CanceledByType canceledByTypeOf(UserRole role) {
        return switch (role) {
            case CUSTOMER -> CanceledByType.CUSTOMER;
            case SELLER -> CanceledByType.SELLER;
            default -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };
    }
}
