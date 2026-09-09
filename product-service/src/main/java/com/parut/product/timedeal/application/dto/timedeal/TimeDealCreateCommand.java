package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


// NOTE: 판매자 직접 등록 전용이라 productId가 없다 — 전환 생성은 상품에서 스냅샷을 받아오는 별도 커맨드가 맡는다.
// NOTE: 구조적 요건(필수값 존재)만 본다. 가격·수량·기간 규칙은 도메인이 지킨다.
public record TimeDealCreateCommand(
        UUID sellerId,
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
        Integer maxPurchaseQuantity,
        Integer initialQuantity,
        Integer lowStockThreshold
) {
    public TimeDealCreateCommand {
        if (sellerId == null
                || name == null
                || productGrade == null
                || origin == null
                || harvestedDate == null
                || originalPrice == null
                || discountRate == null
                || startAt == null
                || endAt == null
                || maxPurchaseQuantity == null
                || initialQuantity == null
                || lowStockThreshold == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
