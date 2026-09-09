package com.parut.product.product.presentation.product.controller;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.common.OffsetResponse;
import com.parut.product.global.common.SortDirection;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.product.query.condition.SellerProductSearchCondition;
import com.parut.product.product.application.product.query.result.SellerProductQueryResult;
import com.parut.product.product.application.product.service.ProductService;
import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.presentation.product.dto.request.SellerProductSearchRequest;
import com.parut.product.product.presentation.product.dto.response.SellerProductListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellerProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private SellerProductController sellerProductController;

    @Test
    void 판매자_상품을_오래된순으로_조회한다() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        SellerProductSearchRequest request = new SellerProductSearchRequest(
                "사과",
                ProductCategory.FRUIT,
                ProductStatus.ON_SALE,
                AppearanceType.NORMAL
        );
        SellerProductQueryResult item = new SellerProductQueryResult(
                productId,
                "청송 사과",
                ProductCategory.FRUIT,
                5_000L,
                ProductStatus.ON_SALE
        );

        given(productService.searchSellerProducts(
                eq(sellerId),
                any(SellerProductSearchCondition.class),
                any(Pageable.class)
        )).willAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(2);
            return new PageImpl<>(List.of(item), pageable, 1);
        });

        ResponseEntity<ApiResponse<OffsetResponse<SellerProductListResponse>>> response =
                sellerProductController.search(sellerId, request, 1, 10, "asc");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().content()).hasSize(1);
        assertThat(response.getBody().data().content().getFirst().productId()).isEqualTo(productId);
        assertThat(response.getBody().data().pageInfo().page()).isEqualTo(1);
        assertThat(response.getBody().data().pageInfo().sort()).isEqualTo("createdAt");
        assertThat(response.getBody().data().pageInfo().direction()).isEqualTo(SortDirection.ASC);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).searchSellerProducts(
                eq(sellerId),
                eq(request.toCondition()),
                pageableCaptor.capture()
        );
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt").isAscending()).isTrue();
    }

    @Test
    void 판매자_상품은_최신순으로_조회한다() {
        UUID sellerId = UUID.randomUUID();
        SellerProductSearchRequest request = new SellerProductSearchRequest(
                null,
                null,
                null,
                null
        );

        given(productService.searchSellerProducts(
                eq(sellerId),
                any(SellerProductSearchCondition.class),
                any(Pageable.class)
        )).willAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(2);
            return new PageImpl<>(List.of(), pageable, 0);
        });

        sellerProductController.search(sellerId, request, 1, 10, "desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).searchSellerProducts(
                eq(sellerId),
                eq(request.toCondition()),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void 판매자_상품_조회에서_page가_1보다_작으면_예외가_발생한다() {
        UUID sellerId = UUID.randomUUID();
        SellerProductSearchRequest request = new SellerProductSearchRequest(
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> sellerProductController.search(
                sellerId,
                request,
                0,
                10,
                "desc"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 판매자_상품_조회에서_허용되지_않은_size면_예외가_발생한다() {
        UUID sellerId = UUID.randomUUID();
        SellerProductSearchRequest request = new SellerProductSearchRequest(
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> sellerProductController.search(
                sellerId,
                request,
                1,
                20,
                "desc"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAGE_SIZE);
    }
}
