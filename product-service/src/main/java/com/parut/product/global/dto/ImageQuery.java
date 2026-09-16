package com.parut.product.global.dto;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

import java.util.UUID;

// NOTE: Image Service 조회 조건이다. 조회 요청이므로 Command가 아니라 Query로 명명한다.
public record ImageQuery(
        UUID timeDealId
) {
    public ImageQuery {
        if (timeDealId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
