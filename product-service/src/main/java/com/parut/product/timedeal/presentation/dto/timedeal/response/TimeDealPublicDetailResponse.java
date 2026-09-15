package com.parut.product.timedeal.presentation.dto.timedeal.response;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView;
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
        UUID imageId,
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

    public static TimeDealPublicDetailResponse from(TimeDealPublicDetailView timeDealPublicDetailView) {
        return new TimeDealPublicDetailResponse(
                timeDealPublicDetailView.timeDealId(),
                timeDealPublicDetailView.productId(),
                timeDealPublicDetailView.sellerId(),
                timeDealPublicDetailView.imageId(),
                timeDealPublicDetailView.name(),
                timeDealPublicDetailView.description(),
                timeDealPublicDetailView.productGrade(),
                timeDealPublicDetailView.origin(),
                timeDealPublicDetailView.harvestedDate(),
                timeDealPublicDetailView.originalPrice(),
                timeDealPublicDetailView.discountRate(),
                timeDealPublicDetailView.dealPrice(),
                timeDealPublicDetailView.startAt(),
                timeDealPublicDetailView.endAt(),
                timeDealPublicDetailView.maxPurchaseQuantity(),
                timeDealPublicDetailView.status(),
                new Stock(
                        timeDealPublicDetailView.availableQuantity(),
                        timeDealPublicDetailView.reservedQuantity(),
                        timeDealPublicDetailView.soldQuantity(),
                        timeDealPublicDetailView.lowStockThreshold()
                )
        );
    }
}
