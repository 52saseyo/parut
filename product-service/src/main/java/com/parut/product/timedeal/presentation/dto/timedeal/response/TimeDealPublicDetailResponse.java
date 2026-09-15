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

    public static TimeDealPublicDetailResponse from(TimeDealPublicDetailResult timeDealPublicDetailResult) {
        return new TimeDealPublicDetailResponse(
                timeDealPublicDetailResult.timeDealId(),
                timeDealPublicDetailResult.productId(),
                timeDealPublicDetailResult.sellerId(),
                timeDealPublicDetailResult.imageUrl(),
                timeDealPublicDetailResult.name(),
                timeDealPublicDetailResult.description(),
                timeDealPublicDetailResult.productGrade(),
                timeDealPublicDetailResult.origin(),
                timeDealPublicDetailResult.harvestedDate(),
                timeDealPublicDetailResult.originalPrice(),
                timeDealPublicDetailResult.discountRate(),
                timeDealPublicDetailResult.dealPrice(),
                timeDealPublicDetailResult.startAt(),
                timeDealPublicDetailResult.endAt(),
                timeDealPublicDetailResult.maxPurchaseQuantity(),
                timeDealPublicDetailResult.status(),
                new Stock(
                        timeDealPublicDetailResult.availableQuantity(),
                        timeDealPublicDetailResult.reservedQuantity(),
                        timeDealPublicDetailResult.soldQuantity(),
                        timeDealPublicDetailResult.lowStockThreshold()
                )
        );
    }
}
