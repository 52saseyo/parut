package com.parut.product.timedeal.presentation.dto.timedeal.response;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailResult;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


// NOTE: order-service가 주문 아이템 스냅샷과 결제 금액을 만들 때 쓰는 내부 응답이다.
public record TimeDealDetailResponse(
        UUID timeDealId,
        UUID productId,
        UUID sellerId,
        String imageUrl,
        String productName,
        String description,
        Long originalPrice,
        BigDecimal discountRate,
        Long dealPrice,
        TimeDealProductGrade productGrade,
        String origin,
        LocalDate harvestedDate
) {
    public static TimeDealDetailResponse from(TimeDealDetailResult result) {
        return new TimeDealDetailResponse(
                result.timeDealId(),
                result.productId(),
                result.sellerId(),
                result.imageUrl(),
                result.productName(),
                result.description(),
                result.originalPrice(),
                result.discountRate(),
                result.dealPrice(),
                result.productGrade(),
                result.origin(),
                result.harvestedDate()
        );
    }
}
