package com.parut.product.product.infrastructure.product.persistence;

import com.parut.product.global.common.SortDirection;
import com.parut.product.product.application.product.query.ProductQueryRepository;
import com.parut.product.product.application.product.query.condition.PublicProductSearchCondition;
import com.parut.product.product.application.product.query.condition.SellerProductSearchCondition;
import com.parut.product.product.application.product.query.result.ProductCursorResult;
import com.parut.product.product.application.product.query.result.PublicProductQueryResult;
import com.parut.product.product.application.product.query.result.SellerProductQueryResult;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.parut.product.product.domain.product.QProduct.product;
import static com.parut.product.product.infrastructure.product.persistence.ProductQueryExpressions.*;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private final JPAQueryFactory queryFactory;


    /**
     * 일반 사용자 상품 목록을 Cursor 방식으로 조회
     * 전체 개수 count 쿼리를 실행하지 않고, size보다 1개 더 조회해서 다음 페이지 존재 여부를 판단
     */
    @Override
    public ProductCursorResult<PublicProductQueryResult> searchPublicProducts(
            PublicProductSearchCondition condition,
            String cursor,
            UUID cursorId,
            int size,
            String sort,
            SortDirection direction
    ) {
        List<Tuple> rows = queryFactory
                .select(
                        product.id,
                        product.name,
                        product.category,
                        product.price,
                        product.appearanceType,
                        product.origin,
                        product.createdAt
                )
                .from(product)
                .where(
                        product.deletedAt.isNull(),
                        publicStatusEq(condition.status()),
                        keywordContains(condition.keyword()),
                        categoryEq(condition.category()),
                        appearanceTypeEq(condition.appearanceType()),
                        priceGoe(condition.minPrice()),
                        priceLoe(condition.maxPrice()),
                        publicCursorCondition(
                                cursor,
                                cursorId,
                                sort,
                                direction
                        )
                )
                .orderBy(
                        createCursorOrderSpecifiers(
                                sort,
                                direction
                        )
                )
                // size보다 1개 더 조회해 다음 페이지가 있는지 판단
                .limit(size + 1L)
                .fetch();

        boolean hasNext = rows.size() > size;

        // 추가 조회한 1개는 응답에서 제외한다.
        List<Tuple> currentRows = hasNext
                ? rows.subList(0, size)
                : rows;

        List<PublicProductQueryResult> content =
                currentRows.stream()
                        .map(this::toPublicQueryResult)
                        .toList();

        // 마지막 페이지에는 더 요청할 위치가 없으므로 Cursor를 null로 반환한다.
        if (!hasNext) {
            return ProductCursorResult.empty(content);
        }
        // 현재 응답의 마지막 상품을 다음 요청의 cursor 기준으로 사용
        Tuple lastRow = currentRows.getLast();

        return ProductCursorResult.of(
                content,
                extractCursor(lastRow, sort),
                lastRow.get(product.id),
                hasNext
        );
    }

    private PublicProductQueryResult toPublicQueryResult(Tuple row) {
        return new PublicProductQueryResult(
                row.get(product.id),
                row.get(product.name),
                row.get(product.category),
                row.get(product.price),
                row.get(product.appearanceType),
                row.get(product.origin)
        );
    }


    /**
     * 다음 페이지 요청에 사용할 cursor 값을 추출
     * createdAt 정렬이면 마지막 상품의 createdAt 값을,
     * price 정렬이면 마지막 상품의 price 값을 cursor로 사용
     */
    private String extractCursor(
            Tuple row,
            String sort
    ) {
        // 다음 요청의 시작점으로 사용할 마지막 상품의 정렬값을 문자열로 반환
        return switch (sort) {
            case "createdAt" ->
                    row.get(product.createdAt).toString();

            case "price" ->
                    row.get(product.price).toString();

            default ->
                    row.get(product.createdAt).toString();
        };
    }


    /**
     * 판매자 본인 상품 목록을 Offset 방식으로 조회
     */
    @Override
    public Page<SellerProductQueryResult> searchSellerProducts(
            UUID sellerId,
            SellerProductSearchCondition condition,
            Pageable pageable
    ) {
        List<SellerProductQueryResult> content = queryFactory
                .select(
                        Projections.constructor(
                                SellerProductQueryResult.class,
                                product.id,
                                product.name,
                                product.category,
                                product.price,
                                product.status
                        )
                )
                .from(product)
                .where(
                        product.deletedAt.isNull(),
                        product.sellerId.eq(sellerId),
                        keywordContains(condition.keyword()),
                        categoryEq(condition.category()),
                        statusEq(condition.status()),
                        appearanceTypeEq(condition.appearanceType())
                )
                .orderBy(
                        // 판매자 조회는 createdAt 기준 최신순/오래된순만 지원
                        createOffsetOrderSpecifiers(pageable)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Offset 응답은 전체 페이지 수 계산이 필요하므로 count 쿼리를 별도로 실행
        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        product.deletedAt.isNull(),
                        product.sellerId.eq(sellerId),
                        keywordContains(condition.keyword()),
                        categoryEq(condition.category()),
                        statusEq(condition.status()),
                        appearanceTypeEq(condition.appearanceType())
                )
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0L : total
        );
    }

}
