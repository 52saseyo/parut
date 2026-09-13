package com.parut.product.timedeal.presentation.dto.timedeal.request;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealUpdateCommand;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// NOTE: PATCH 부분 수정. 생략과 명시적 null 모두 변경하지 않음을 뜻한다.
public record TimeDealUpdateRequest(
        UUID imageId,
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
    public TimeDealUpdateCommand toCommand(UUID timeDealId, UUID requesterId, String requesterRole) {
        return new TimeDealUpdateCommand(
                timeDealId, requesterId, requesterRole,
                imageId, name, description, productGrade, origin, harvestedDate, originalPrice, discountRate, startAt, endAt, maxPurchaseQuantity
        );
    }
}
