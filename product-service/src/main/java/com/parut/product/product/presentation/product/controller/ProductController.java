package com.parut.product.product.presentation.product.controller;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.common.OffsetPageInfo;
import com.parut.product.global.common.OffsetResponse;
import com.parut.product.global.common.SortDirection;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.product.service.ProductService;
import com.parut.product.product.presentation.product.dto.request.*;
import com.parut.product.product.presentation.product.dto.response.ProductDetailResponse;
import com.parut.product.product.presentation.product.dto.response.PublicProductListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.Set;

@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@RestController
public class ProductController {
    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);
    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "updatedAt", "price");

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

    @GetMapping
    public ResponseEntity<ApiResponse<OffsetResponse<PublicProductListResponse>>> search(
            @ModelAttribute PublicProductSearchCondition condition,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ){
        validatePagination(page, size, sort);
        SortDirection resolvedDirection = resolveDirection(direction);
        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(resolvedDirection.toSpringDirection(), sort)
        );

        Page<PublicProductListResponse> result = productService.searchPublicProducts(condition, pageable);
        OffsetResponse<PublicProductListResponse> response = new OffsetResponse<>(
                result.getContent(),
                OffsetPageInfo.of(
                        page,
                        size,
                        sort,
                        resolvedDirection,
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.isLast()
                )
        );
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    private void validatePagination(int page, int size, String sort) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!ALLOWED_SIZES.contains(size)) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new BusinessException(ErrorCode.INVALID_SORT_FIELD);
        }
    }

    private SortDirection resolveDirection(String direction) {
        return "asc".equalsIgnoreCase(direction)
                ? SortDirection.ASC
                : SortDirection.DESC;
    }

}
