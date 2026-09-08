package com.parut.product.product.application.product.query;

import com.parut.product.product.presentation.product.dto.request.PublicProductSearchCondition;
import com.parut.product.product.presentation.product.dto.request.SellerProductSearchCondition;
import com.parut.product.product.presentation.product.dto.response.PublicProductListResponse;
import com.parut.product.product.presentation.product.dto.response.SellerProductListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductQueryRepository {

    Page<PublicProductListResponse> searchPublicProducts(
            PublicProductSearchCondition condition,
            Pageable pageable
    );

    Page<SellerProductListResponse> searchSellerProducts(
            UUID sellerId,
            SellerProductSearchCondition condition,
            Pageable pageable
    );
}
