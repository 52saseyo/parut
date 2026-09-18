package com.parut.product.image.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImageUploadCompleteRequest(
        @NotBlank(message = "이미지 키는 필수입니다.")
        @Size(
                max = 500,
                message = "이미지 키는 500자를 넘을 수 없습니다."
        )
        String imageKey,

        @NotBlank(message = "원본 파일명은 필수입니다.")
        @Size(
                max = 255,
                message = "원본 파일명은 255자를 넘을 수 없습니다."
        )
        String originalName
) {
}
