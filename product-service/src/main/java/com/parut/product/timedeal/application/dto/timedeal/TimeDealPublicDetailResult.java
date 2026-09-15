package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// NOTE: DB 조회 View와 외부 Image Service 조회 결과를 Application 계층에서 조합한 결과다.
public record TimeDealPublicDetailResult(
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
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity,
        Integer lowStockThreshold
) {
    public static TimeDealPublicDetailResult from(
            TimeDealPublicDetailView timeDealPublicDetailView,
            String imageUrl
    ) {
        return new TimeDealPublicDetailResult(
                timeDealPublicDetailView.timeDealId(),
                timeDealPublicDetailView.productId(),
                timeDealPublicDetailView.sellerId(),
                imageUrl,
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
                timeDealPublicDetailView.availableQuantity(),
                timeDealPublicDetailView.reservedQuantity(),
                timeDealPublicDetailView.soldQuantity(),
                timeDealPublicDetailView.lowStockThreshold()
        );
    }
}
