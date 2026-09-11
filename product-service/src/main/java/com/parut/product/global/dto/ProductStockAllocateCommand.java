package com.parut.product.global.dto;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

import java.util.UUID;


// NOTE: 일반 상품 재고를 타임딜 재고로 할당할 때 timedeal이 product에 넘기는 요청.
// HTTP를 거치지 않아 헤더가 없으므로 요청자 정보도 파라미터로 함께 넘긴다.
public record ProductStockAllocateCommand(
        UUID productId,
        Integer quantity,
        UUID requesterId,
        String requesterRole
) {
    public ProductStockAllocateCommand {
        if (productId == null || quantity == null || requesterId == null || requesterRole == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}