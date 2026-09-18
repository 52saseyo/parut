package com.parut.product.product.presentation.product.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterProductImageRequest(
        @NotNull(message = "이미지 ID는 필수입니다.")
        UUID imageId
) {
}
