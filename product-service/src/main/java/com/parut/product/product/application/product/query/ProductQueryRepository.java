package com.parut.product.product.application.product.query;

import com.parut.product.global.common.SortDirection;
import com.parut.product.product.application.product.query.condition.PublicProductSearchCondition;
import com.parut.product.product.application.product.query.condition.SellerProductSearchCondition;
import com.parut.product.product.application.product.query.result.ProductCursorResult;
import com.parut.product.product.application.product.query.result.PublicProductQueryResult;
import com.parut.product.product.application.product.query.result.SellerProductQueryResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductQueryRepository {

    ProductCursorResult<PublicProductQueryResult> searchPublicProducts(
            PublicProductSearchCondition condition,
            String cursor,
            UUID cursorId,
            int size,
            String sort,
            SortDirection direction
    );

    Page<SellerProductQueryResult> searchSellerProducts(
            UUID sellerId,
            SellerProductSearchCondition condition,
            Pageable pageable
    );
}
