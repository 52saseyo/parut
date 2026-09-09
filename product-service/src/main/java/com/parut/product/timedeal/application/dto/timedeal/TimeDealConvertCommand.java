package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


// NOTE: 앞의 네 값은 product의 allocate()로 그대로 넘어간다. 표시 정보와 originalPrice·sellerId는
// 여기 없다 — 전부 할당 결과로 받아온다(요청에서 받으면 원가·소유자 위조가 가능하다).
// NOTE: 뒤의 다섯 값은 이번 딜의 판매 조건이라 product가 알 수 없어 요청에서 받는다.
// NOTE: 구조적 요건(필수값 존재)만 본다. 가격·수량·기간 규칙은 도메인이 지킨다.
public record TimeDealConvertCommand(
        UUID productId,
        Integer quantity,
        UUID requesterId,
        String requesterRole,
        BigDecimal discountRate,
        Instant startAt,
        Instant endAt,
        Integer maxPurchaseQuantity,
        Integer lowStockThreshold
) {
    public TimeDealConvertCommand {
        if (productId == null
                || quantity == null
                || requesterId == null
                || requesterRole == null
                || discountRate == null
                || startAt == null
                || endAt == null
                || maxPurchaseQuantity == null
                || lowStockThreshold == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public ProductStockAllocateCommand toAllocateCommand(){
        return new ProductStockAllocateCommand(productId, quantity, requesterId, requesterRole);
    }
}