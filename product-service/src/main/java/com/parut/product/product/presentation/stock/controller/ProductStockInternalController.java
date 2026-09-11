package com.parut.product.product.presentation.stock.controller;


import com.parut.product.global.common.ApiResponse;
import com.parut.product.product.application.stock.service.ProductStockService;
import com.parut.product.product.presentation.stock.dto.request.ProductStockConfirmRequest;
import com.parut.product.product.presentation.stock.dto.request.ProductStockReserveRequest;
import com.parut.product.product.presentation.stock.dto.request.ProductStockRestoreRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/stocks")
@RequiredArgsConstructor
public class ProductStockInternalController {

    private final ProductStockService productStockService;

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<ApiResponse<Void>> reserve(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductStockReserveRequest request
    ) {
        productStockService.reserve(
                productId, request.orderId(), request.orderItemId(), request.quantity()
        );
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

    @PostMapping("/{productId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductStockConfirmRequest request
    ) {
        productStockService.confirm(productId, request.orderId(), request.orderItemId());
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

    @PostMapping("/{productId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductStockRestoreRequest request
    ) {
        productStockService.restore(productId, request.orderId(), request.orderItemId());
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

}
