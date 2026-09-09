package com.parut.product.timedeal.presentation.dto.timedeal.request;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealConvertCommand;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


// NOTE: 원가·판매자·표시 정보를 받지 않는다 — 전부 product의 할당 결과에서 가져온다.
// NOTE: 필수값 존재만 본다. 할인율 범위, 기간 역전 같은 규칙은 도메인이 지킨다.
public record TimeDealConvertRequest(

        @NotNull(message = "상품 ID는 필수입니다.")
        UUID productId,

        @NotNull(message = "전환할 수량은 필수입니다.")
        Integer quantity,

        @NotNull(message = "할인율은 필수입니다.")
        BigDecimal discountRate,

        @NotNull(message = "판매 시작 일시는 필수입니다.")
        Instant startAt,

        @NotNull(message = "판매 종료 일시는 필수입니다.")
        Instant endAt,

        @NotNull(message = "최대 구매 수량은 필수입니다.")
        Integer maxPurchaseQuantity,

        @NotNull(message = "재고 부족 임계 수량은 필수입니다.")
        Integer lowStockThreshold
) {

    // NOTE: 변환을 Request에 두는 이유는 의존 방향이다 — Command가 Request를 알면 역전이다.
    public TimeDealConvertCommand toCommand(UUID requesterId, String requesterRole) {
        return new TimeDealConvertCommand(
                productId,
                quantity,
                requesterId,
                requesterRole,
                discountRate,
                startAt,
                endAt,
                maxPurchaseQuantity,
                lowStockThreshold
        );
    }
}