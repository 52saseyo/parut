package com.parut.product.product.infrastructure.product.persistence;

import com.parut.product.global.common.SortDirection;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import static com.parut.product.product.domain.product.QProduct.product;

/**
 * 상품 목록 조회에서 공통으로 사용하는 QueryDSL 조건과 정렬식을 생성한다.
 * 검색 조건 생성 로직을 별도 클래스로 분리
 */
final class ProductQueryExpressions {

    // 일반 사용자에게 노출 가능한 상품 상태
    // status 조건이 없으면 ON_SALE, SOLD_OUT 상품만 조회
    private static final List<ProductStatus> PUBLIC_VISIBLE_STATUSES =
            List.of(
                    ProductStatus.ON_SALE,
                    ProductStatus.SOLD_OUT
            );

    private ProductQueryExpressions() {
    }

    /**
     * 상품명 키워드 검색 조건을 생성
     * keyword가 없으면 조건을 적용하지 않는다.
     */
    static BooleanExpression keywordContains(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return product.name.containsIgnoreCase(keyword.trim());
    }

    /**
     * 카테고리 필터 조건을 생성
     */
    static BooleanExpression categoryEq(ProductCategory category) {
        return category == null ? null : product.category.eq(category);
    }

    /**
     * 외형 타입 필터 조건을 생성합니다.
     */
    static BooleanExpression appearanceTypeEq(AppearanceType appearanceType) {
        return appearanceType == null ? null : product.appearanceType.eq(appearanceType);
    }

    /**
     * 상품 상태 필터 조건을 생성
     * 판매자 조회에서는 DRAFT, ON_SALE, SOLD_OUT 등 전체 상태를 사용할 수 있으므로
     * 전달된 status 값을 그대로 조건에 반영
     */
    static BooleanExpression statusEq(ProductStatus status) {
        return status == null ? null : product.status.eq(status);
    }


    /**
     * 일반 사용자 상품 상태 필터 조건을 생성
     *
     * status가 없으면 일반 사용자에게 노출 가능한 ON_SALE, SOLD_OUT만 조회
     * status가 있으면 Service에서 허용 상태인지 검증한 뒤 해당 상태만 조회
     */
    static BooleanExpression publicStatusEq(ProductStatus status) {

        if (status == null) {
            return product.status.in(PUBLIC_VISIBLE_STATUSES);
        }

        return product.status.eq(status);
    }

    /**
     * 최소 가격 조건을 생성
     */
    static BooleanExpression priceGoe(Long minPrice) {
        return minPrice == null ? null : product.price.goe(minPrice);
    }

    /**
     * 최대 가격 조건을 생성
     */
    static BooleanExpression priceLoe(Long maxPrice) {
        return maxPrice == null ? null : product.price.loe(maxPrice);
    }



    /**
     * 일반 사용자 Cursor 페이지네이션 조건을 생성
     *
     * 첫 조회에서는 cursor와 cursorId가 없으므로 조건을 적용하지 않고
     * 다음 조회부터는 이전 응답의 nextCursor, nextIdAfter 값을 기준으로
     * 그 다음 상품 목록을 조회
     */
    static BooleanExpression publicCursorCondition(
            String cursor,
            UUID cursorId,
            String sort,
            SortDirection direction
    ){

        if (cursor == null && cursorId == null) {
            return null;
        }

        if (cursor == null || cursorId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try {

            return switch (sort) {
                case "createdAt" -> createdAtCursorCondition(
                        Instant.parse(cursor),
                        cursorId,
                        direction
                );

                case "price" -> priceCursorCondition(
                        Long.parseLong(cursor),
                        cursorId,
                        direction
                );

                default -> throw new BusinessException(
                        ErrorCode.INVALID_SORT_FIELD
                );
            };
        } catch (DateTimeParseException | NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * createdAt 기준 Cursor 조건을 생성한다.
     *
     * ASC:
     * - createdAt이 cursor보다 큰 상품
     * - createdAt이 같으면 id가 cursorId보다 큰 상품
     *
     * DESC:
     * - createdAt이 cursor보다 작은 상품
     * - createdAt이 같으면 id가 cursorId보다 작은 상품
     */
    private static BooleanExpression createdAtCursorCondition(
            Instant cursor,
            UUID cursorId,
            SortDirection direction
    ) {
        if (direction == SortDirection.ASC) {
            return product.createdAt.gt(cursor)
                    .or(product.createdAt.eq(cursor)
                            .and(product.id.gt(cursorId))
                    );
        }

        return product.createdAt.lt(cursor)
                .or(product.createdAt.eq(cursor)
                        .and(product.id.lt(cursorId))
                );
    }

    /**
     * 일반 사용자 Cursor 조회용 정렬 조건을 생성
     *
     * 일반 사용자는 createdAt 정렬과 price 정렬을 지원
     * id를 두 번째 정렬 기준으로 두어 createdAt이나 price가 같은 상품도
     * 항상 같은 순서로 조회되도록 함
     */
    private static BooleanExpression priceCursorCondition(
            Long cursor,
            UUID cursorId,
            SortDirection direction
    ) {

        if (direction == SortDirection.ASC) {
            return product.price.gt(cursor)
                    .or(product.price.eq(cursor)
                            .and(product.id.gt(cursorId))
                    );
        }

        return product.price.lt(cursor)
                .or(product.price.eq(cursor)
                        .and(product.id.lt(cursorId))
                );
    }


    /**
     * Cursor 조회용 정렬 조건을 생성
     *
     * 첫 번째 정렬 기준은 요청 sort 값이고,
     * 두 번째 정렬 기준은 id
     *
     * id를 함께 정렬하는 이유는 createdAt이나 price가 같은 상품들의 순서를
     * 항상 동일하게 유지하기 위함
     */
    static OrderSpecifier<?>[] createCursorOrderSpecifiers(
            String sort,
            SortDirection direction
    ) {
        boolean ascending = direction == SortDirection.ASC;

        OrderSpecifier<?> primaryOrder = switch (sort) {
            case "createdAt" -> ascending ? product.createdAt.asc() : product.createdAt.desc();
            case "price" -> ascending ? product.price.asc() : product.price.desc();

            default -> product.createdAt.desc();
        };


        OrderSpecifier<?> idOrder = ascending
                ? product.id.asc()
                : product.id.desc();

        return new OrderSpecifier<?>[]{
                primaryOrder,
                idOrder
        };
    }


    /**
     * Offset 조회용 정렬 조건을 생성
     * 판매자 조회에서는 Pageable에 담긴 Sort 정보를 QueryDSL OrderSpecifier로 변환
     */
    static OrderSpecifier<?>[] createOffsetOrderSpecifiers(
            Pageable pageable
    ) {

        Sort.Order order = pageable.getSort()
                .stream()
                .findFirst()
                .orElse(Sort.Order.desc("createdAt"));

        boolean ascending = order.isAscending();

        OrderSpecifier<?> primaryOrder = switch (order.getProperty()) {
            case "createdAt" -> ascending ? product.createdAt.asc() : product.createdAt.desc();
            default -> product.createdAt.desc();
        };

        OrderSpecifier<?> idOrder = ascending ? product.id.asc() : product.id.desc();

        return new OrderSpecifier<?>[]{primaryOrder, idOrder};
    }
}
