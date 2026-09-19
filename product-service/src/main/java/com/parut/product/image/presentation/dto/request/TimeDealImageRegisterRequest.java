package com.parut.product.image.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TimeDealImageRegisterRequest(
        @NotNull(message = "이미지 ID는 필수입니다.")
        UUID imageId
) {
}
