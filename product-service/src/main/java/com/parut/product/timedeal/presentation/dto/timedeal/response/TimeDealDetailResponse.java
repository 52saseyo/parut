package com.parut.product.timedeal.presentation.dto.timedeal.response;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;

import java.time.LocalDate;
import java.util.UUID;


// NOTE: order-service가 주문 아이템 스냅샷과 결제 금액을 만들 때 쓰는 내부 응답이다.
public record TimeDealDetailResponse(
        UUID timeDealId,
        UUID productId,
        UUID sellerId,
        UUID imageId,
        String productName,
        Long originalPrice,
        Long dealPrice,
        TimeDealProductGrade productGrade,
        String origin,
        LocalDate harvestedDate
) {
    public static TimeDealDetailResponse from(TimeDealDetailView timeDealDetailView) {
        return new TimeDealDetailResponse(
                timeDealDetailView.timeDealId(),
                timeDealDetailView.productId(),
                timeDealDetailView.sellerId(),
                timeDealDetailView.imageId(),
                timeDealDetailView.productName(),
                timeDealDetailView.originalPrice(),
                timeDealDetailView.dealPrice(),
                timeDealDetailView.productGrade(),
                timeDealDetailView.origin(),
                timeDealDetailView.harvestedDate()
        );
    }
}