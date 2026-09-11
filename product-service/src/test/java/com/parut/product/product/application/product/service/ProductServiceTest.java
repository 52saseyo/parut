package com.parut.product.product.application.product.service;

import com.parut.product.global.common.SortDirection;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.product.query.ProductQueryRepository;
import com.parut.product.product.application.product.query.condition.PublicProductSearchCondition;
import com.parut.product.product.application.product.query.condition.SellerProductSearchCondition;
import com.parut.product.product.application.product.query.result.ProductCursorResult;
import com.parut.product.product.application.product.query.result.PublicProductQueryResult;
import com.parut.product.product.application.product.query.result.SellerProductQueryResult;
import com.parut.product.product.application.stock.service.ProductStockService;
import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.domain.product.SaleUnit;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.infrastructure.product.persistence.ProductRepository;
import com.parut.product.product.presentation.product.dto.request.CreateProductRequest;
import com.parut.product.product.presentation.product.dto.request.UpdateProductRequest;
import com.parut.product.product.presentation.product.dto.response.ProductDetailResponse;
import com.parut.product.product.presentation.product.dto.response.ProductOrderInfoResponse;
import com.parut.product.product.presentation.product.dto.response.ProductResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final UUID SELLER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductStockService productStockService;

    @Mock
    private ProductQueryRepository productQueryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void 상품을_생성하고_초기_재고를_생성한다() {
        CreateProductRequest request = new CreateProductRequest(
                ProductCategory.FRUIT,
                "못난이 사과",
                "상품 설명",
                3_000L,
                AppearanceType.UGLY,
                "충주",
                LocalDate.of(2026, 9, 1),
                SaleUnit.KG,
                new BigDecimal("1.00"),
                100,
                10
        );
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            setId(product, PRODUCT_ID);
            return product;
        });

        ProductResponse response = productService.createProduct(SELLER_ID, request);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.status()).isEqualTo(ProductStatus.DRAFT);
        verify(productStockService).createStock(PRODUCT_ID, 100, 10);
    }

    @Test
    void 판매자가_상품_정보를_수정한다() {
        Product product = product();
        UpdateProductRequest request = new UpdateProductRequest(
                ProductCategory.VEGETABLE,
                "수정 상품",
                null,
                5_000L,
                AppearanceType.NORMAL,
                "제주",
                null,
                null,
                null
        );
        given(productRepository.findByIdAndSellerIdAndDeletedAtIsNull(PRODUCT_ID, SELLER_ID))
                .willReturn(Optional.of(product));

        ProductResponse response = productService.updateProduct(PRODUCT_ID, SELLER_ID, request);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(product.getName()).isEqualTo("수정 상품");
        assertThat(product.getCategory()).isEqualTo(ProductCategory.VEGETABLE);
        assertThat(product.getPrice()).isEqualTo(5_000L);
        assertThat(product.getOrigin()).isEqualTo("제주");
    }

    @Test
    void 없는_상품을_수정하면_예외가_발생한다() {
        UpdateProductRequest request = new UpdateProductRequest(
                null, "수정 상품", null, null, null, null, null, null, null
        );
        given(productRepository.findByIdAndSellerIdAndDeletedAtIsNull(PRODUCT_ID, SELLER_ID))
                .willReturn(Optional.empty());

        assertBusinessException(
                () -> productService.updateProduct(PRODUCT_ID, SELLER_ID, request),
                ErrorCode.PRODUCT_NOT_FOUND
        );
    }

    @Test
    void 상품과_재고를_소프트_삭제한다() {
        Product product = product();
        given(productRepository.findByIdAndSellerIdAndDeletedAtIsNull(PRODUCT_ID, SELLER_ID))
                .willReturn(Optional.of(product));

        productService.deleteProduct(PRODUCT_ID, SELLER_ID);

        verify(productStockService).deleteStock(PRODUCT_ID, SELLER_ID.toString());
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
        assertThat(product.isDeleted()).isTrue();
    }

    @Test
    void 재고가_있으면_판매를_시작한다() {
        Product product = product();
        product.addImage(UUID.randomUUID());
        ProductStock stock = ProductStock.create(PRODUCT_ID, 100, 10);
        given(productRepository.findByIdAndSellerIdAndDeletedAtIsNull(PRODUCT_ID, SELLER_ID))
                .willReturn(Optional.of(product));
        given(productStockService.getStock(PRODUCT_ID)).willReturn(stock);

        ProductResponse response = productService.updateProductStatus(
                PRODUCT_ID,
                SELLER_ID,
                ProductStatus.ON_SALE
        );

        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void 판매_가능한_재고가_없으면_판매를_시작할_수_없다() {
        Product product = product();
        ProductStock stock = ProductStock.create(PRODUCT_ID, 10, 1);
        stock.reserve(10);
        given(productRepository.findByIdAndSellerIdAndDeletedAtIsNull(PRODUCT_ID, SELLER_ID))
                .willReturn(Optional.of(product));
        given(productStockService.getStock(PRODUCT_ID)).willReturn(stock);

        assertBusinessException(
                () -> productService.updateProductStatus(PRODUCT_ID, SELLER_ID, ProductStatus.ON_SALE),
                ErrorCode.PRODUCT_STOCK_PRODUCT_NOT_ON_SALE
        );
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void 직접_변경할_수_없는_상품_상태는_거부한다() {
        Product product = product();
        given(productRepository.findByIdAndSellerIdAndDeletedAtIsNull(PRODUCT_ID, SELLER_ID))
                .willReturn(Optional.of(product));

        assertBusinessException(
                () -> productService.updateProductStatus(PRODUCT_ID, SELLER_ID, ProductStatus.SOLD_OUT),
                ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED
        );
        verify(productStockService, never()).getStock(PRODUCT_ID);
    }

    @Test
    void 공개_상태인_상품_상세를_조회한다() {
        Product product = onSaleProduct();
        given(productRepository.findByIdAndStatusInAndDeletedAtIsNull(
                PRODUCT_ID,
                List.of(ProductStatus.ON_SALE, ProductStatus.SOLD_OUT)
        )).willReturn(Optional.of(product));

        ProductDetailResponse response = productService.getProduct(PRODUCT_ID);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void 공개되지_않은_상품_상세는_조회할_수_없다() {
        given(productRepository.findByIdAndStatusInAndDeletedAtIsNull(
                PRODUCT_ID,
                List.of(ProductStatus.ON_SALE, ProductStatus.SOLD_OUT)
        )).willReturn(Optional.empty());

        assertBusinessException(
                () -> productService.getProduct(PRODUCT_ID),
                ErrorCode.PRODUCT_NOT_FOUND
        );
    }

    @Test
    void 판매자가_본인_상품_상세를_조회한다() {
        Product product = product();
        given(productRepository.findByIdAndSellerIdAndDeletedAtIsNull(PRODUCT_ID, SELLER_ID))
                .willReturn(Optional.of(product));

        ProductDetailResponse response = productService.getMyProduct(SELLER_ID, PRODUCT_ID);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.status()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void 판매중이고_재고가_있으면_주문_가능한_상품_정보를_반환한다() {
        Product product = onSaleProduct();
        ProductStock stock = ProductStock.create(PRODUCT_ID, 100, 10);
        UUID stockId = UUID.randomUUID();
        setId(stock, stockId);
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(product));
        given(productStockService.getStock(PRODUCT_ID)).willReturn(stock);

        ProductOrderInfoResponse response = productService.getOrderInfo(PRODUCT_ID);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.stockId()).isEqualTo(stockId);
        assertThat(response.purchasable()).isTrue();
    }

    @Test
    void 공개_가능한_상태의_상품_검색을_Repository에_위임한다() {
        PublicProductSearchCondition condition = publicSearchCondition(ProductStatus.ON_SALE);
        ProductCursorResult<PublicProductQueryResult> expected =
                ProductCursorResult.empty(List.of());
        given(productQueryRepository.searchPublicProducts(
                condition, null, null, 10, "createdAt", SortDirection.DESC
        )).willReturn(expected);

        ProductCursorResult<PublicProductQueryResult> result =
                productService.searchPublicProducts(
                        condition, null, null, 10, "createdAt", SortDirection.DESC
                );

        assertThat(result).isSameAs(expected);
        verify(productQueryRepository).searchPublicProducts(
                condition, null, null, 10, "createdAt", SortDirection.DESC
        );
    }

    @Test
    void 공개할_수_없는_상태는_검색하지_않는다() {
        PublicProductSearchCondition condition = publicSearchCondition(ProductStatus.DRAFT);

        assertBusinessException(
                () -> productService.searchPublicProducts(
                        condition, null, null, 10, "createdAt", SortDirection.DESC
                ),
                ErrorCode.INVALID_INPUT_VALUE
        );
        verify(productQueryRepository, never()).searchPublicProducts(
                condition, null, null, 10, "createdAt", SortDirection.DESC
        );
    }

    @Test
    void 판매자_상품_검색을_Repository에_위임한다() {
        SellerProductSearchCondition condition = new SellerProductSearchCondition(
                "사과",
                ProductCategory.FRUIT,
                ProductStatus.ON_SALE,
                AppearanceType.NORMAL
        );
        Pageable pageable = PageRequest.of(0, 10);
        Page<SellerProductQueryResult> expected = new PageImpl<>(List.of(), pageable, 0);
        given(productQueryRepository.searchSellerProducts(SELLER_ID, condition, pageable))
                .willReturn(expected);

        Page<SellerProductQueryResult> result =
                productService.searchSellerProducts(SELLER_ID, condition, pageable);

        assertThat(result).isSameAs(expected);
        verify(productQueryRepository).searchSellerProducts(SELLER_ID, condition, pageable);
    }

    private Product product() {
        Product product = Product.create(
                SELLER_ID,
                ProductCategory.FRUIT,
                "못난이 사과",
                "상품 설명",
                3_000L,
                AppearanceType.UGLY,
                "충주",
                LocalDate.of(2026, 9, 1),
                SaleUnit.KG,
                new BigDecimal("1.00")
        );
        setId(product, PRODUCT_ID);
        return product;
    }

    private Product onSaleProduct() {
        Product product = product();
        product.addImage(UUID.randomUUID());
        product.startSale();
        return product;
    }

    private PublicProductSearchCondition publicSearchCondition(ProductStatus status) {
        return new PublicProductSearchCondition(
                "사과",
                ProductCategory.FRUIT,
                AppearanceType.NORMAL,
                status,
                1_000L,
                10_000L
        );
    }

    private void setId(Object entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    private void assertBusinessException(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode)
                );
    }
}
