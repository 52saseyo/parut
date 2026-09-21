package com.parut.product.image.presentation.controller;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.image.application.service.TimeDealImageService;
import com.parut.product.image.presentation.dto.request.TimeDealImageRegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/time-deals/seller/{timeDealId}/images")
public class TimeDealImageController {

    private final TimeDealImageService timeDealImageService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> registerImage(
            @PathVariable UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @Valid @RequestBody TimeDealImageRegisterRequest request
    ) {
        timeDealImageService.registerImage(requesterId, requesterRole, timeDealId, request.imageId());
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }
}
