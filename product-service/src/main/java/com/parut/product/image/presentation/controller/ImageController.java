package com.parut.product.image.presentation.controller;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.image.application.dto.ImageUploadUrlResult;
import com.parut.product.image.application.service.ImageService;
import com.parut.product.image.presentation.dto.request.ImageUploadCompleteRequest;
import com.parut.product.image.presentation.dto.request.ImageUploadUrlRequest;
import com.parut.product.image.presentation.dto.response.ImageResponse;
import com.parut.product.image.presentation.dto.response.ImageUploadUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {
    private final ImageService imageService;

    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<ImageUploadUrlResponse>> createPresignedUrl(
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @Valid @RequestBody ImageUploadUrlRequest request
    ) {
        ImageUploadUrlResult result = imageService.createUploadUrl(
                requesterId,
                requesterRole,
                request
        );
        ImageUploadUrlResponse response = ImageUploadUrlResponse.from(result);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<ImageResponse>> completeUpload(
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @Valid @RequestBody ImageUploadCompleteRequest request
    ){
        ImageResponse response = imageService.completeUpload(
                requesterId,
                requesterRole,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, null));
    }
}
