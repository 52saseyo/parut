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

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<Void>> reserve(
            @Valid @RequestBody ProductStockReserveRequest request
    ) {
        productStockService.reserve(request.orderId(), request.toItems());
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(
            @Valid @RequestBody ProductStockConfirmRequest request
    ) {
        productStockService.confirm(request.orderId(), request.toItems());
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

    @PostMapping("/restore")
    public ResponseEntity<ApiResponse<Void>> restore(
            @Valid @RequestBody ProductStockRestoreRequest request
    ) {
        productStockService.restore(request.orderId(), request.toItems());
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

}
