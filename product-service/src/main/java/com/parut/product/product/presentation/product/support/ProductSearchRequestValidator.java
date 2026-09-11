package com.parut.product.product.presentation.product.support;

import com.parut.product.global.common.SortDirection;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

import java.util.Set;
import java.util.UUID;

/**
 * 상품 목록 API가 허용하는 페이지 크기와 정렬 조건을 공통으로 검증한다.
 *
 * 일반 사용자 Cursor 조회와 판매자 Offset 조회에서
 * page, size, sort, cursor 조건을 Controller마다 중복 검증하지 않도록 분리
 * */
public final class ProductSearchRequestValidator {

    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);
    private static final Set<String> PUBLIC_ALLOWED_SORTS = Set.of("createdAt", "price");

    private ProductSearchRequestValidator() {
    }


    /**
     * 일반 사용자 상품 목록 Cursor 조회 요청을 검증
     * 둘 중 하나만 전달되면 잘못된 요청으로 처리
     */
    public static void validateCursorRequest(String cursor, UUID cursorId, int size, String sort) {
        validateSize(size);
        validatePublicSort(sort);

        if ((cursor == null) != (cursorId == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     *  판매자 상품 목록 Offset 조회 요청을 검증
     */
    public static void validateSellerOffsetRequest(int page, int size) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        validateSize(size);
    }

    /**
     *
     * 가격 필터 조건을 검증
     */
    public static void validatePriceRange(Long minPrice, Long maxPrice) {
        if (minPrice != null && minPrice < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * direction 요청값을 정렬 방향으로 변환
     */
    public static SortDirection resolveDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? SortDirection.ASC : SortDirection.DESC;
    }

    /**
     * size 검증
     */
    private static void validateSize(int size) {
        if (!ALLOWED_SIZES.contains(size)) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }
    }

    /**
     * Sort 검증
     */
    private static void validatePublicSort(String sort) {
        if (!PUBLIC_ALLOWED_SORTS.contains(sort)) {
            throw new BusinessException(ErrorCode.INVALID_SORT_FIELD);
        }
    }

}
