package com.parut.product.image.presentation.dto.request;

import jakarta.validation.constraints.*;

public record RegisterProductImageRequest(
        @NotBlank(message = "이미지 키는 필수입니다.")
        @Size(max = 500, message = "이미지 키는 500자를 초과할 수 없습니다.")
        String imageKey,

        @NotBlank(message = "원본 파일명은 필수입니다.")
        @Size(max = 255, message = "원본 파일명은 255자를 초과할 수 없습니다.")
        String originalName,

        @NotBlank(message = "이미지 콘텐츠 타입은 필수입니다.")
        String contentType,

        @NotNull(message = "파일 크기는 필수입니다.")
        @Positive(message = "파일 크기는 0보다 커야 합니다.")
        @Max(
                value = 10_485_760,
                message = "이미지 크기는 10MB 이하여야 합니다."
        )
        Long fileSize
)
{
}
