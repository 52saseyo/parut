package com.parut.product.product.presentation.stock.controller;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.common.OffsetPageInfo;
import com.parut.product.global.common.OffsetResponse;
import com.parut.product.global.common.SortDirection;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.stock.service.ProductStockService;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.presentation.stock.dto.request.ProductStockUpdateRequest;
import com.parut.product.product.presentation.stock.dto.response.ProductStockResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class ProductStockController {

    private static final List<Integer> ALLOWED_SIZES = List.of(10, 30, 50);
    private static final List<String> ALLOWED_SORTS = List.of("createdAt", "updatedAt");

    private final ProductStockService productStockService;

    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductStockResponse>> updateStock(
            @PathVariable UUID productId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ProductStockUpdateRequest request
    ) {

        productStockService.updateStock(productId, request.totalQuantity());
        ProductStock stock = productStockService.getStock(productId);
        return ResponseEntity.ok(ApiResponse.success(ProductStockResponse.from(stock), null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OffsetResponse<ProductStockResponse>>> getStockList(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        if (!ALLOWED_SIZES.contains(size)) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_PAGE_INVALID_SIZE);
        }
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_SORT_INVALID_FIELD);
        }

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        // page는 1부터 시작, Pageable은 0부터 시작이라 -1 보정
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDirection, sort));

        Page<ProductStock> stockPage = productStockService.getStockList(pageable);

        List<ProductStockResponse> content = stockPage.getContent().stream()
                .map(ProductStockResponse::from)
                .toList();

        OffsetPageInfo pageInfo = OffsetPageInfo.of(
                page,
                size,
                sort,
                SortDirection.valueOf(direction.toUpperCase()),
                stockPage.getTotalElements(),
                stockPage.getTotalPages(),
                stockPage.isLast()
        );

        OffsetResponse<ProductStockResponse> response = new OffsetResponse<>(content, pageInfo);

        return ResponseEntity.ok(ApiResponse.success(response, null));
    }
}
