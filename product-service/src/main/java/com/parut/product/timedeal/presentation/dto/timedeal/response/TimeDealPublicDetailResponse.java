package com.parut.product.timedeal.presentation.dto.timedeal.response;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.time.Instant;
import java.util.UUID;

public record TimeDealPublicDetailResponse(
        UUID timeDealId,
        UUID productId,
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
