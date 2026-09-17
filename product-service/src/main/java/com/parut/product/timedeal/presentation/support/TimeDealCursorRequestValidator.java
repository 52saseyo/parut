package com.parut.product.timedeal.presentation.support;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.util.Set;
import java.util.UUID;

public final class TimeDealCursorRequestValidator {

    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);

    private TimeDealCursorRequestValidator() {
    }

    public static void validate(TimeDealStatus status, String cursor, UUID cursorId, int size) {
        if (status != TimeDealStatus.SCHEDULED && status != TimeDealStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!ALLOWED_SIZES.contains(size)) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }
        if ((cursor == null) != (cursorId == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
