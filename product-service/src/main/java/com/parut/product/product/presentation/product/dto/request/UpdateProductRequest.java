package com.parut.product.product.presentation.product.dto.request;

import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.SaleUnit;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProductRequest(
        ProductCategory category,

        @Pattern(regexp = ".*\\S.*", message = "상품명은 공백일 수 없습니다.")
        @Size(max = 150, message = "상품명은 150자를 초과할 수 없습니다.")
        String name,

        String description,

        @PositiveOrZero(message = "상품 가격은 0 이상이어야 합니다.")
        Long price,

        AppearanceType appearanceType,

        @Pattern(regexp = ".*\\S.*", message = "원산지는 공백일 수 없습니다.")
        @Size(max = 100, message = "원산지는 100자를 초과할 수 없습니다.")
        String origin,

        LocalDate harvestDate,

        SaleUnit saleUnit,

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
        BigDecimal unitQuantity
) {
}
