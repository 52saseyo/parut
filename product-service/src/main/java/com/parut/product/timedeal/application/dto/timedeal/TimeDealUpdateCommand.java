package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// NOTE: 수정 필드의 null은 기존 값 유지다. 요청자와 대상 식별자만 필수다.
public record TimeDealUpdateCommand(
        UUID timeDealId,
        UUID requesterId,
        String requesterRole,
        String name,
        String description,
        TimeDealProductGrade productGrade,
        String origin,
        LocalDate harvestedDate,
        Long originalPrice,
        BigDecimal discountRate,
        Instant startAt,
        Instant endAt,
        Integer maxPurchaseQuantity
) {
    public TimeDealUpdateCommand {
        if (timeDealId == null || requesterId == null || requesterRole == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
