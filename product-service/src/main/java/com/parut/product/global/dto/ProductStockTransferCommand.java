package com.parut.product.global.dto;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

import java.util.UUID;

// NOTE: 타임딜과 일반 상품 사이의 재고 이동을 Product 컨텍스트에 요청하는 내부 명령이다.
// quantity는 타임딜 기준 방향을 반대로 바꿔 Product 컨텍스트에 전달한다.
public record ProductStockTransferCommand(
        UUID productId,
        Integer quantity,
        UUID requesterId,
        String requesterRole
) {
    public ProductStockTransferCommand {
        if (productId == null || quantity == null || requesterId == null || requesterRole == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public static ProductStockTransferCommand of(
            UUID productId,
            Integer quantity,
            UUID requesterId,
            String requesterRole
    ) {
        return new ProductStockTransferCommand(productId, -quantity, requesterId, requesterRole); // NOTE: quantity 부호를 각 서비스마다 반대로 사용하기때문에 -를 붙여 혼동이없도록한다
    }
}
