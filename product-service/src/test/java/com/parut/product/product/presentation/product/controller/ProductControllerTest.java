package com.parut.product.product.presentation.product.controller;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.common.CursorResponse;
import com.parut.product.global.common.SortDirection;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.product.query.condition.PublicProductSearchCondition;
import com.parut.product.product.application.product.query.result.ProductCursorResult;
import com.parut.product.product.application.product.query.result.PublicProductQueryResult;
import com.parut.product.product.application.product.service.ProductService;
import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.presentation.product.dto.request.PublicProductSearchRequest;
import com.parut.product.product.presentation.product.dto.response.PublicProductListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @Test
    void 공개_상품을_커서_방식으로_조회한다() {
        UUID productId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        PublicProductSearchRequest request = new PublicProductSearchRequest(
                "사과",
                ProductCategory.FRUIT,
                AppearanceType.UGLY,
                ProductStatus.ON_SALE,
                1_000L,
                10_000L
        );
        PublicProductQueryResult item = new PublicProductQueryResult(
                productId,
                "못난이 사과",
                ProductCategory.FRUIT,
                3_000L,
                AppearanceType.UGLY,
                "충주"
        );

        given(productService.searchPublicProducts(
                any(PublicProductSearchCondition.class),
                isNull(),
                isNull(),
                eq(10),
                eq("createdAt"),
                eq(SortDirection.DESC)
        )).willReturn(ProductCursorResult.of(
                List.of(item),
                "2026-09-08T04:00:00Z",
                nextId,
                true
        ));

        ResponseEntity<ApiResponse<CursorResponse<PublicProductListResponse>>> response =
                productController.search(request, null, null, 10, "createdAt", "desc");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().content()).hasSize(1);
        assertThat(response.getBody().data().content().getFirst().productId()).isEqualTo(productId);
        assertThat(response.getBody().data().pageInfo().nextIdAfter()).isEqualTo(nextId);
        assertThat(response.getBody().data().pageInfo().hasNext()).isTrue();

        ArgumentCaptor<PublicProductSearchCondition> conditionCaptor =
                ArgumentCaptor.forClass(PublicProductSearchCondition.class);
        verify(productService).searchPublicProducts(
                conditionCaptor.capture(),
                isNull(),
                isNull(),
                eq(10),
                eq("createdAt"),
                eq(SortDirection.DESC)
        );
        assertThat(conditionCaptor.getValue()).isEqualTo(request.toCondition());
    }

    @Test
    void 공개_상품을_가격_낮은순으로_조회한다() {
        PublicProductSearchRequest request = new PublicProductSearchRequest(
                null,
                null,
                null,
                ProductStatus.ON_SALE,
                null,
                null
        );

        given(productService.searchPublicProducts(
                any(PublicProductSearchCondition.class),
                isNull(),
                isNull(),
                eq(30),
                eq("price"),
                eq(SortDirection.ASC)
        )).willReturn(ProductCursorResult.empty(List.of()));

        ResponseEntity<ApiResponse<CursorResponse<PublicProductListResponse>>> response =
                productController.search(request, null, null, 30, "price", "asc");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().pageInfo().sortBy()).isEqualTo("price");
        assertThat(response.getBody().data().pageInfo().sortDirection()).isEqualTo(SortDirection.ASC);

        verify(productService).searchPublicProducts(
                any(PublicProductSearchCondition.class),
                isNull(),
                isNull(),
                eq(30),
                eq("price"),
                eq(SortDirection.ASC)
        );
    }

    @Test
    void 커서와_커서아이디_중_하나만_있으면_예외가_발생한다() {
        PublicProductSearchRequest request = new PublicProductSearchRequest(
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> productController.search(
                request,
                "2026-09-08T04:00:00Z",
                null,
                10,
                "createdAt",
                "desc"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 허용되지_않은_size면_예외가_발생한다() {
        PublicProductSearchRequest request = new PublicProductSearchRequest(
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> productController.search(
                request,
                null,
                null,
                20,
                "createdAt",
                "desc"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAGE_SIZE);
    }
}
