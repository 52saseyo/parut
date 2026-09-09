package com.parut.product.image.presentation.controller;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.image.application.service.ProductImageService;
import com.parut.product.image.presentation.dto.request.RegisterProductImageRequest;
import com.parut.product.image.presentation.dto.response.ImageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seller/products")
public class ProductImageController {
    private final ProductImageService productImageService;


    @PostMapping("/{productId}/image")
    public ResponseEntity<ApiResponse<ImageResponse>> addImage(
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @PathVariable UUID productId,
            @Valid @RequestBody RegisterProductImageRequest request
    ) {
        ImageResponse response = productImageService.addImage(
                sellerId,
                productId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, null));
    }
}
