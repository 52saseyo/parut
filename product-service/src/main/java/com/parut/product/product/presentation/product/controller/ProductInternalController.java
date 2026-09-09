package com.parut.product.product.presentation.product.controller;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.product.application.product.service.ProductService;
import com.parut.product.product.presentation.product.dto.response.ProductOrderInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/products")
public class ProductInternalController {
    private final ProductService productService;

    /**
     * 주문 서비스 등 내부 서비스에서 주문 가능 여부와 상품 정보를 조회한다.
     */
    @GetMapping("/{productId}/order-info")
    public ResponseEntity<ApiResponse<ProductOrderInfoResponse>> getInfo(
            @PathVariable UUID productId
    ){
        ProductOrderInfoResponse response = productService.getOrderInfo(productId);

        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

}
