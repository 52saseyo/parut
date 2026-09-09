package com.parut.product.product.presentation.product.dto.request;

import com.parut.product.product.domain.product.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(
        @NotNull(message = "변경할 상품 상태는 필수입니다.")
        ProductStatus status
) {
}
