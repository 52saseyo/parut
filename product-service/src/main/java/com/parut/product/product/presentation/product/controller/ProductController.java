package com.parut.product.product.presentation.product.controller;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.product.application.product.service.ProductService;
import com.parut.product.product.presentation.product.dto.request.*;
import com.parut.product.product.presentation.product.dto.response.ProductDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@RestController
public class ProductController {
    private final ProductService productService;


    /**
     * 구매자 또는 비로그인 사용자가 볼 수 있는 공개 상품 상세를 조회한다.
     * 판매 중이거나 품절된 상품만 조회된다.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getOne(
            @PathVariable UUID productId
    ){
        ProductDetailResponse response = productService.getProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }






}
