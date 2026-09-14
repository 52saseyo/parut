package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

import java.util.UUID;

public record TimeDealDeleteCommand(UUID timeDealId, UUID requesterId, String requesterRole) {
    public TimeDealDeleteCommand {
        if (timeDealId == null || requesterId == null || requesterRole == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
