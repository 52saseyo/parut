package com.parut.product.product.presentation.product.controller;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.product.application.product.service.ProductService;
import com.parut.product.product.presentation.product.dto.request.CreateProductRequest;
import com.parut.product.product.presentation.product.dto.request.UpdateProductRequest;
import com.parut.product.product.presentation.product.dto.request.UpdateProductStatusRequest;
import com.parut.product.product.presentation.product.dto.response.ProductDetailResponse;
import com.parut.product.product.presentation.product.dto.response.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/v1/seller/products")
@RequiredArgsConstructor
@RestController
public class SellerProductController {
    private final ProductService productService;

    /**
     * 판매자가 상품 기본 정보와 초기 재고를 함께 등록한다.
     * 생성된 상품은 판매 준비 상태로 저장된다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @Valid @RequestBody CreateProductRequest request
    ){
        ProductResponse response = productService.createProduct(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, null));
    }

    /**
     * 판매자가 본인 상품의 기본 정보를 수정한다.
     * 수정 가능 상태 검증은 도메인에서 처리한다.
     */
    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable UUID productId,
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @Valid @RequestBody UpdateProductRequest request
    ){
        ProductResponse response = productService.updateProduct(productId, sellerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    /**
     * 판매자가 본인 상품을 소프트 삭제한다.
     * 상품 삭제 시 연결된 재고와 이미지도 함께 삭제 처리된다.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID productId,
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId
    ){
        productService.deleteProduct(productId, sellerId);
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }


    /**
     * 판매자가 상품 판매 상태를 변경한다.
     * 현재 API에서는 판매 시작/재개와 판매 중지만 허용한다.
     */
    @PatchMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStatus(
            @PathVariable UUID productId,
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @Valid @RequestBody UpdateProductStatusRequest request
    ){
        ProductResponse response = productService.updateProductStatus(
                productId,
                sellerId,
                request.status()
        );
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    /**
     * 판매자가 본인 상품 상세를 조회한다.
     * 공개 조회와 달리 판매 준비, 판매 중지 상태의 상품도 조회할 수 있다.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getOne(
            @PathVariable UUID productId,
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId
    ){
        ProductDetailResponse response = productService.getMyProduct(sellerId, productId);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }
}
