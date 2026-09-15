package com.parut.product.timedeal.presentation.dto.timedeal.response;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailResult;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TimeDealPublicDetailResponse(
        UUID timeDealId,
        UUID productId,
        UUID sellerId,
        String imageUrl,
        String name,
        String description,
        TimeDealProductGrade productGrade,
        String origin,
        LocalDate harvestedDate,
        Long originalPrice,
        BigDecimal discountRate,
        Long dealPrice,
        Instant startAt,
        Instant endAt,
        Integer maxPurchaseQuantity,
        TimeDealStatus status,
        Stock stock
) {
    public record Stock(
            Integer availableQuantity,
            Integer reservedQuantity,
            Integer soldQuantity,
            Integer lowStockThreshold
    ) {
    }

    public static TimeDealPublicDetailResponse from(TimeDealPublicDetailResult result) {
        return new TimeDealPublicDetailResponse(
                result.timeDealId(),
                result.productId(),
                result.sellerId(),
                result.imageUrl(),
                result.name(),
                result.description(),
                result.productGrade(),
                result.origin(),
                result.harvestedDate(),
                result.originalPrice(),
                result.discountRate(),
                result.dealPrice(),
                result.startAt(),
                result.endAt(),
                result.maxPurchaseQuantity(),
                result.status(),
                new Stock(
                        result.availableQuantity(),
                        result.reservedQuantity(),
                        result.soldQuantity(),
                        result.lowStockThreshold()
                )
        );
    }
}
