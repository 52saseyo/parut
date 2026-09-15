package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// NOTE: DB 조회 View와 외부 Image Service 조회 결과를 Application 계층에서 조합한 결과다.
public record TimeDealDetailResult(
        UUID timeDealId, UUID productId, UUID sellerId, String imageUrl,
        String productName, String description, Long originalPrice,
        BigDecimal discountRate, Long dealPrice, TimeDealProductGrade productGrade,
        String origin, LocalDate harvestedDate
) {
    public static TimeDealDetailResult from(TimeDealDetailView view, String imageUrl) {
        return new TimeDealDetailResult(
                view.timeDealId(), view.productId(), view.sellerId(), imageUrl,
                view.productName(), view.description(), view.originalPrice(),
                view.discountRate(), view.dealPrice(), view.productGrade(),
                view.origin(), view.harvestedDate());
    }
}
