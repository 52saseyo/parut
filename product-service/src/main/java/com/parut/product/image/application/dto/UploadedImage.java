package com.parut.product.image.application.dto;

public record UploadedImage(
        String imageKey,
        String imageUrl,
        String contentType,
        long fileSize
) {
}
