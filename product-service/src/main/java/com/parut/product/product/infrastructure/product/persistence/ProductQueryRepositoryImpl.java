package com.parut.product.product.infrastructure.product.persistence;

import com.parut.product.product.application.product.query.ProductQueryRepository;
import com.parut.product.product.domain.product.ProductImageType;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.presentation.product.dto.request.PublicProductSearchCondition;
import com.parut.product.product.presentation.product.dto.request.SellerProductSearchCondition;
import com.parut.product.product.presentation.product.dto.response.PublicProductListResponse;
import com.parut.product.product.presentation.product.dto.response.SellerProductListResponse;
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
import static com.parut.product.product.domain.product.QProductImage.productImage;
import static com.parut.product.product.infrastructure.product.persistence.ProductQueryExpressions.*;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private static final List<ProductStatus> PUBLIC_VISIBLE_STATUSES =
            List.of(
                    ProductStatus.ON_SALE,
                    ProductStatus.SOLD_OUT
            );
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PublicProductListResponse> searchPublicProducts(
            PublicProductSearchCondition condition,
            Pageable pageable
    ) {
        List<PublicProductListResponse> content = queryFactory
                .select(
                        Projections.constructor(
                                PublicProductListResponse.class,
                                product.id,
                                product.name,
                                product.category,
                                product.price,
                                product.appearanceType,
                                product.origin,
                                productImage.imageKey
                        )
                )
                .from(product)

                .leftJoin(productImage)
                .on(
                        productImage.product.id.eq(product.id),
                        productImage.imageType.eq(ProductImageType.MAIN),
                        productImage.deletedAt.isNull()
                )
                .where(
                        product.deletedAt.isNull(),
                        product.status.in(PUBLIC_VISIBLE_STATUSES),
                        keywordContains(condition.keyword()),
                        categoryEq(condition.category()),
                        appearanceTypeEq(condition.appearanceType()),
                        priceGoe(condition.minPrice()),
                        priceLoe(condition.maxPrice())
                )

                .orderBy(
                        createOrderSpecifiers(pageable)
                )

                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        product.deletedAt.isNull(),
                        product.status.in(PUBLIC_VISIBLE_STATUSES),
                        keywordContains(condition.keyword()),
                        categoryEq(condition.category()),
                        appearanceTypeEq(condition.appearanceType()),
                        priceGoe(condition.minPrice()),
                        priceLoe(condition.maxPrice())
                )
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );

    }

    @Override
    public Page<SellerProductListResponse> searchSellerProducts(
            UUID sellerId,
            SellerProductSearchCondition condition,
            Pageable pageable)
    {
        List<SellerProductListResponse> content =
                queryFactory
                        .select(
                                Projections.constructor(
                                        SellerProductListResponse.class,
                                        product.id,
                                        product.name,
                                        product.category,
                                        product.price,
                                        product.status,
                                        productImage.imageKey
                                )
                        )
                        .from(product)

                        .leftJoin(productImage)
                        .on(
                                productImage.product.id.eq(product.id),
                                productImage.imageType.eq(ProductImageType.MAIN),
                                productImage.deletedAt.isNull()
                        )

                        .where(
                                product.deletedAt.isNull(),
                                product.sellerId.eq(sellerId),
                                keywordContains(condition.keyword()),
                                categoryEq(condition.category()),
                                statusEq(condition.status()),
                                priceGoe(condition.minPrice()),
                                priceLoe(condition.maxPrice())
                        )

                        .orderBy(
                                createOrderSpecifiers(pageable)
                        )

                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();
        Long total =
                queryFactory
                        .select(product.count())
                        .from(product)

                        .where(
                                product.deletedAt.isNull(),
                                product.sellerId.eq(sellerId),
                                keywordContains(condition.keyword()),
                                categoryEq(condition.category()),
                                statusEq(condition.status()),
                                priceGoe(condition.minPrice()),
                                priceLoe(condition.maxPrice())
                        )
                        .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0
        );
    }

}
