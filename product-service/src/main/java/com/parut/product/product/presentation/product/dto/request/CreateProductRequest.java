package com.parut.product.product.presentation.product.dto.request;

import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.SaleUnit;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProductRequest(
        @NotNull(message = "카테고리는 필수입니다.")
        ProductCategory category,

        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 150, message = "상품명은 150자를 초과할 수 없습니다.")
        String name,

        String description,

        @NotNull(message = "상품 가격은 필수입니다.")
        @PositiveOrZero(message = "상품 가격은 0 이상이어야 합니다.")
        Long price,

        @NotNull(message = "외관 유형은 필수입니다.")
        AppearanceType appearanceType,

        @NotBlank(message = "원산지는 필수입니다.")
        @Size(max = 100, message = "원산지는 100자를 초과할 수 없습니다.")
        String origin,

        @NotNull(message = "수확일은 필수입니다.")
        LocalDate harvestDate,

        @NotNull(message = "판매 단위는 필수입니다.")
        SaleUnit saleUnit,

        @NotNull(message = "판매 단위 수량은 필수입니다.")
        @Digits(
                integer = 8,
                fraction = 2,
                message = "판매 단위 수량은 정수 8자리, 소수 2자리까지 입력할 수 있습니다."
        )
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "판매 단위 수량은 0보다 커야 합니다."
        )
        BigDecimal unitQuantity,

        @NotNull(message = "초기 재고 수량은 필수입니다.")
        @PositiveOrZero(message = "초기 재고 수량은 0 이상이어야 합니다.")
        Integer totalQuantity,

        @NotNull(message = "재고 부족 기준 수량은 필수입니다.")
        @PositiveOrZero(message = "재고 부족 기준 수량은 0 이상이어야 합니다.")
        Integer lowStockThreshold
) {
}
