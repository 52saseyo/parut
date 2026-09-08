package com.parut.product.product.infrastructure.product.persistence;

import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static com.parut.product.product.domain.product.QProduct.product;

/**
 * 상품 목록 조회에서 공통으로 사용하는 QueryDSL 조건과 정렬식을 생성한다.
 */
final class ProductQueryExpressions {

    private ProductQueryExpressions() {
    }

    static BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return product.name.containsIgnoreCase(keyword.trim());
    }

    static BooleanExpression categoryEq(ProductCategory category) {
        return category == null ? null : product.category.eq(category);
    }

    static BooleanExpression appearanceTypeEq(AppearanceType appearanceType) {
        return appearanceType == null ? null : product.appearanceType.eq(appearanceType);
    }

    static BooleanExpression statusEq(ProductStatus status) {
        return status == null ? null : product.status.eq(status);
    }

    static BooleanExpression priceGoe(Long minPrice) {
        return minPrice == null ? null : product.price.goe(minPrice);
    }

    static BooleanExpression priceLoe(Long maxPrice) {
        return maxPrice == null ? null : product.price.loe(maxPrice);
    }

    static OrderSpecifier<?>[] createOrderSpecifiers(Pageable pageable) {
        Sort.Order order = pageable.getSort()
                .stream()
                .findFirst()
                .orElse(Sort.Order.desc("createdAt"));

        boolean ascending = order.isAscending();
        OrderSpecifier<?> primaryOrder = switch (order.getProperty()) {
            case "createdAt" -> ascending
                    ? product.createdAt.asc()
                    : product.createdAt.desc();
            case "updatedAt" -> ascending
                    ? product.updatedAt.asc()
                    : product.updatedAt.desc();
            case "price" -> ascending
                    ? product.price.asc()
                    : product.price.desc();
            default -> product.createdAt.desc();
        };

        return new OrderSpecifier<?>[]{primaryOrder, product.id.desc()};
    }
}
