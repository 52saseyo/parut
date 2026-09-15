package com.parut.product.global.dto;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

import java.util.UUID;

// NOTE: 타임딜과 이미지의 연결을 Image Service에 저장할 때 사용하는 내부 명령이다.
public record TimeDealImageSaveCommand(
        UUID timeDealId,
        UUID imageId
) {
    public TimeDealImageSaveCommand {
        if (timeDealId == null || imageId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
