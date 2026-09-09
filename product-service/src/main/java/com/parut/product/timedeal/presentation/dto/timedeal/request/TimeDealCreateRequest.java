package com.parut.product.timedeal.presentation.dto.timedeal.request;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateCommand;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


// NOTE: 필수값 존재만 본다 — 가격 하한, 할인율 범위, 기간 역전 같은 규칙은 도메인이 지킨다(양쪽에 두면 갈린다).
public record TimeDealCreateRequest(

        UUID imageId,

        @NotNull(message = "타임딜 상품명은 필수입니다.")
        String name,

        String description,

        @NotNull(message = "타임딜 상품 품질은 필수입니다.")
        TimeDealProductGrade productGrade,

        @NotNull(message = "생산지는 필수입니다.")
        String origin,

        @NotNull(message = "수확일은 필수입니다.")
        Instant harvestedAt,

        @NotNull(message = "정가는 필수입니다.")
        Long originalPrice,

        @NotNull(message = "할인율은 필수입니다.")
        BigDecimal discountRate,

        @NotNull(message = "판매 시작 일시는 필수입니다.")
        Instant startAt,

        @NotNull(message = "판매 종료 일시는 필수입니다.")
        Instant endAt,

        @NotNull(message = "최대 구매 수량은 필수입니다.")
        Integer maxPurchaseQuantity,

        @NotNull(message = "초기 재고 수량은 필수입니다.")
        Integer initialQuantity,

        @NotNull(message = "재고 부족 임계 수량은 필수입니다.")
        Integer lowStockThreshold
) {

    // NOTE: 변환을 Request에 두는 이유는 의존 방향이다 — Command가 Request를 알면 역전이다.
    public TimeDealCreateCommand toCommand(UUID sellerId) {
        return new TimeDealCreateCommand(
                sellerId,
                imageId,
                name,
                description,
                productGrade,
                origin,
                harvestedAt,
                originalPrice,
                discountRate,
                startAt,
                endAt,
                maxPurchaseQuantity,
                initialQuantity,
                lowStockThreshold
        );
    }
}
