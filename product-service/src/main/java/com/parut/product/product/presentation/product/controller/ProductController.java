package com.parut.product.product.presentation.product.controller;

import com.parut.product.global.common.*;
import com.parut.product.product.application.product.query.result.ProductCursorResult;
import com.parut.product.product.application.product.query.result.PublicProductQueryResult;
import com.parut.product.product.application.product.service.ProductService;
import com.parut.product.product.presentation.product.dto.request.PublicProductSearchRequest;
import com.parut.product.product.presentation.product.dto.response.ProductDetailResponse;
import com.parut.product.product.presentation.product.dto.response.PublicProductListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

import static com.parut.product.product.presentation.product.support.ProductSearchRequestValidator.*;

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

    @GetMapping
    /** 일반 사용자 상품 검색: 첫 요청에는 Cursor를 생략하고 다음 요청부터 응답값을 전달한다. */
    public ResponseEntity<ApiResponse<CursorResponse<PublicProductListResponse>>> search(
            @ModelAttribute PublicProductSearchRequest request,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ){
        validateCursorRequest(cursor, cursorId, size, sort);
        validatePriceRange(request.minPrice(), request.maxPrice());

        SortDirection resolvedDirection = resolveDirection(direction);


        ProductCursorResult<PublicProductQueryResult> result = productService.searchPublicProducts(
                        request.toCondition(),
                        cursor,
                        cursorId,
                        size,
                        sort,
                        resolvedDirection
        );

        CursorResponse<PublicProductListResponse> response = new CursorResponse<>(
                        result.content().stream()
                                .map(PublicProductListResponse::from)
                                .toList(),
                        CursorPageInfo.of(
                                result.nextCursor(),
                                result.nextIdAfter(),
                                result.hasNext(),
                                sort,
                                resolvedDirection
                        )
        );

        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

}
