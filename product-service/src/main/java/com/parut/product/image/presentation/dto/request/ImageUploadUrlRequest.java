package com.parut.product.image.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ImageUploadUrlRequest(
        @NotBlank(message = "콘텐츠 타입은 필수입니다.")
        String contentType,

        @NotNull(message = "파일 크기는 필수입니다.")
        @Positive(message = "파일 크기는 0보다 커야 합니다.")
        @Max(
                value = 10_485_760,
                message = "이미지는 10MB 이하여야 합니다."
        )
        Long fileSize
) {
}
