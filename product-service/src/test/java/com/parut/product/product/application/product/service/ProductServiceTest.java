package com.parut.product.product.application.product.service;

import com.parut.product.global.common.SortDirection;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.authorization.product.ProductAuthorizationChecker;
import com.parut.product.product.application.product.port.out.ProductImagePort;
import com.parut.product.product.application.product.port.out.dto.ProductImageResult;
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
import com.parut.product.product.domain.stock.enums.StockStatus;
import com.parut.product.product.infrastructure.product.persistence.ProductRepository;
import com.parut.product.product.presentation.product.dto.request.CreateProductRequest;
import com.parut.product.product.presentation.product.dto.request.UpdateProductRequest;
import com.parut.product.product.presentation.product.dto.response.ProductDetailResponse;
import com.parut.product.product.presentation.product.dto.response.ProductOrderInfoResponse;
import com.parut.product.product.presentation.product.dto.response.ProductResponse;
import com.parut.product.product.presentation.product.dto.response.SellerProductDetailResponse;
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
import java.util.Map;
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
    private static final UUID IMAGE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final String IMAGE_URL = "https://example.com/images/apple.jpg";
    private static final UUID PRODUCT_ID_2 = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final String SELLER_ROLE = "SELLER";
    private static final List<ProductStatus> CUSTOMER_VISIBLE_STATUSES =
            List.of(ProductStatus.ON_SALE, ProductStatus.SOLD_OUT);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductStockService productStockService;

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private ProductImagePort productImagePort;

    @Mock
    private ProductAuthorizationChecker authorizationChecker;

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
                10,
                IMAGE_ID
        );
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            setId(product, PRODUCT_ID);
            return product;
        });

        ProductResponse response = productService.createProduct(SELLER_ID, SELLER_ROLE, request);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.status()).isEqualTo(ProductStatus.DRAFT);
        verify(productStockService).createStock(PRODUCT_ID, 100, 10);
        verify(productImagePort).save(SELLER_ID, PRODUCT_ID, IMAGE_ID);
        verify(authorizationChecker).requireSeller(SELLER_ROLE);
    }

    @Test
    void 이미지_없이도_상품과_초기_재고를_생성한다() {
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
                10,
                null
        );
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            setId(product, PRODUCT_ID);
            return product;
        });

        ProductResponse response = productService.createProduct(SELLER_ID, SELLER_ROLE, request);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        verify(productStockService).createStock(PRODUCT_ID, 100, 10);
        verify(productImagePort, never()).save(any(), any(), any());
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
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(product));

        ProductResponse response = productService.updateProduct(PRODUCT_ID, SELLER_ID, SELLER_ROLE, request);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(product.getName()).isEqualTo("수정 상품");
        assertThat(product.getCategory()).isEqualTo(ProductCategory.VEGETABLE);
        assertThat(product.getPrice()).isEqualTo(5_000L);
        assertThat(product.getOrigin()).isEqualTo("제주");
        verify(authorizationChecker).requireSellerOwner(SELLER_ID, SELLER_ROLE, SELLER_ID);
    }

    @Test
    void 없는_상품을_수정하면_예외가_발생한다() {
        UpdateProductRequest request = new UpdateProductRequest(
                null, "수정 상품", null, null, null, null, null, null, null
        );
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.empty());

        assertBusinessException(
                () -> productService.updateProduct(PRODUCT_ID, SELLER_ID, SELLER_ROLE, request),
                ErrorCode.PRODUCT_NOT_FOUND
        );
    }

    @Test
    void 상품과_재고를_소프트_삭제한다() {
        Product product = product();
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(product));

        productService.deleteProduct(PRODUCT_ID, SELLER_ID, SELLER_ROLE);

        verify(productStockService).deleteStock(PRODUCT_ID, SELLER_ID.toString());
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
        assertThat(product.isDeleted()).isTrue();
    }

    @Test
    void 이미지가_없어도_재고가_있으면_판매를_시작한다() {
        Product product = product();
        ProductStock stock = ProductStock.create(PRODUCT_ID, 100, 10);
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(product));
        given(productStockService.getStock(PRODUCT_ID)).willReturn(stock);

        ProductResponse response = productService.updateProductStatus(
                PRODUCT_ID, SELLER_ID, SELLER_ROLE, ProductStatus.ON_SALE);

        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        verify(productImagePort, never()).hasImage(PRODUCT_ID);
    }

    @Test
    void 이미지가_없어도_판매를_재개한다() {
        Product product = onSaleProduct();
        product.suspend();
        ProductStock stock = ProductStock.create(PRODUCT_ID, 100, 10);
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(product));
        given(productStockService.getStock(PRODUCT_ID)).willReturn(stock);

        ProductResponse response = productService.updateProductStatus(
                PRODUCT_ID, SELLER_ID, SELLER_ROLE, ProductStatus.ON_SALE);

        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
        verify(productImagePort, never()).hasImage(PRODUCT_ID);
    }

    @Test
    void 판매_가능한_재고가_없으면_판매를_시작할_수_없다() {
        Product product = product();
        ProductStock stock = ProductStock.create(PRODUCT_ID, 10, 1);
        stock.reserve(10);
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(product));
        given(productStockService.getStock(PRODUCT_ID)).willReturn(stock);

        assertBusinessException(
                () -> productService.updateProductStatus(PRODUCT_ID, SELLER_ID, SELLER_ROLE, ProductStatus.ON_SALE),
                ErrorCode.PRODUCT_STOCK_PRODUCT_NOT_ON_SALE
        );
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void 직접_변경할_수_없는_상품_상태는_거부한다() {
        Product product = product();
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(product));

        assertBusinessException(
                () -> productService.updateProductStatus(PRODUCT_ID, SELLER_ID, SELLER_ROLE, ProductStatus.SOLD_OUT),
                ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED
        );
        verify(productStockService, never()).getStock(PRODUCT_ID);
    }

    @Test
    void 공개_상태인_상품_상세를_재고와_함께_조회한다() {
        Product product = onSaleProduct();
        ProductStock stock = ProductStock.create(PRODUCT_ID, 100, 10);
        stock.reserve(5);

        given(productRepository.findByIdAndStatusInAndDeletedAtIsNull(
                PRODUCT_ID,
                CUSTOMER_VISIBLE_STATUSES
        )).willReturn(Optional.of(product));
        given(productImagePort.findImage(PRODUCT_ID))
                .willReturn(Optional.of(new ProductImageResult(IMAGE_ID, IMAGE_URL)));
        given(productStockService.getStock(PRODUCT_ID)).willReturn(stock);

        ProductDetailResponse response = productService.getProduct(PRODUCT_ID);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(response.availableQuantity()).isEqualTo(95);
        assertThat(response.imageUrl()).isEqualTo(IMAGE_URL);
    }

    @Test
    void 공개_상품에_이미지가_없어도_상세를_조회한다() {
        Product product = onSaleProduct();
        ProductStock stock = ProductStock.create(PRODUCT_ID, 100, 10);
        given(productRepository.findByIdAndStatusInAndDeletedAtIsNull(
                PRODUCT_ID,
                CUSTOMER_VISIBLE_STATUSES
        )).willReturn(Optional.of(product));
        given(productStockService.getStock(PRODUCT_ID)).willReturn(stock);

        ProductDetailResponse response = productService.getProduct(PRODUCT_ID);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.imageUrl()).isNull();
    }

    @Test
    void 존재하지_않는_상품_상세는_조회할_수_없다() {
        given(productRepository.findByIdAndStatusInAndDeletedAtIsNull(
                PRODUCT_ID,
                CUSTOMER_VISIBLE_STATUSES
        )).willReturn(Optional.empty());

        assertBusinessException(
                () -> productService.getProduct(PRODUCT_ID),
                ErrorCode.PRODUCT_NOT_FOUND
        );
        verify(productImagePort, never()).findImage(PRODUCT_ID);
    }

    @Test
    void 판매자가_본인_상품_상세를_조회한다() {
        Product product = product();
        ProductStock stock = ProductStock.create(PRODUCT_ID, 100, 10);
        setId(stock, UUID.randomUUID());
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(product));
        given(productStockService.getStock(PRODUCT_ID)).willReturn(stock);

        SellerProductDetailResponse response =
                productService.getMyProduct(PRODUCT_ID, SELLER_ID, SELLER_ROLE);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(response.totalQuantity()).isEqualTo(100);
        assertThat(response.availableQuantity()).isEqualTo(100);
        assertThat(response.lowStockThreshold()).isEqualTo(10);
        assertThat(response.stockStatus()).isEqualTo(StockStatus.AVAILABLE);
        assertThat(response.url()).isNull();
    }

    @Test
    void 판매중인_본인_상품에_이미지가_없어도_상세를_조회한다() {
        Product product = onSaleProduct();
        ProductStock stock = ProductStock.create(PRODUCT_ID, 100, 10);
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(product));
        given(productStockService.getStock(PRODUCT_ID)).willReturn(stock);

        SellerProductDetailResponse response =
                productService.getMyProduct(PRODUCT_ID, SELLER_ID, SELLER_ROLE);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(response.url()).isNull();
    }

    @Test
    void 본인_상품에_이미지를_연결한다() {
        given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(product()));

        productService.registerImage(PRODUCT_ID, SELLER_ID, SELLER_ROLE, IMAGE_ID);

        verify(productImagePort).save(SELLER_ID, PRODUCT_ID, IMAGE_ID);
    }

    @Test
    void 없는_상품에는_이미지를_연결할_수_없다() {
        assertBusinessException(
                () -> productService.registerImage(PRODUCT_ID, SELLER_ID, SELLER_ROLE, IMAGE_ID),
                ErrorCode.PRODUCT_NOT_FOUND
        );
        verify(productImagePort, never()).save(any(), any(), any());
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
    void 여러_상품의_주문_정보를_요청한_순서대로_반환한다() {
        Product firstProduct = onSaleProduct(PRODUCT_ID, "사과");
        Product secondProduct = onSaleProduct(PRODUCT_ID_2, "배");
        ProductStock firstStock = ProductStock.create(PRODUCT_ID, 100, 10);
        ProductStock secondStock = ProductStock.create(PRODUCT_ID_2, 0, 0);
        UUID firstStockId = UUID.randomUUID();
        UUID secondStockId = UUID.randomUUID();
        setId(firstStock, firstStockId);
        setId(secondStock, secondStockId);

        List<UUID> requestedProductIds = List.of(PRODUCT_ID, PRODUCT_ID_2);

        // IN 조회 결과의 순서가 요청 순서와 다르더라도 최종 응답 순서는 요청을 따라야 한다.
        given(productRepository.findByIdInAndDeletedAtIsNull(requestedProductIds))
                .willReturn(List.of(secondProduct, firstProduct));
        given(productStockService.getStocks(requestedProductIds))
                .willReturn(List.of(secondStock, firstStock));

        List<ProductOrderInfoResponse> responses =
                productService.getOrderInfos(requestedProductIds);

        assertThat(responses)
                .extracting(ProductOrderInfoResponse::productId)
                .containsExactly(PRODUCT_ID, PRODUCT_ID_2);
        assertThat(responses)
                .extracting(ProductOrderInfoResponse::stockId)
                .containsExactly(firstStockId, secondStockId);
        assertThat(responses)
                .extracting(ProductOrderInfoResponse::purchasable)
                .containsExactly(true, false);
    }

    @Test
    void 다건_조회에서_중복된_상품_ID는_한_번만_조회하고_반환한다() {
        Product firstProduct = onSaleProduct(PRODUCT_ID, "사과");
        Product secondProduct = onSaleProduct(PRODUCT_ID_2, "배");
        ProductStock firstStock = ProductStock.create(PRODUCT_ID, 100, 10);
        ProductStock secondStock = ProductStock.create(PRODUCT_ID_2, 50, 5);
        setId(firstStock, UUID.randomUUID());
        setId(secondStock, UUID.randomUUID());

        List<UUID> distinctProductIds = List.of(PRODUCT_ID, PRODUCT_ID_2);
        given(productRepository.findByIdInAndDeletedAtIsNull(distinctProductIds))
                .willReturn(List.of(firstProduct, secondProduct));
        given(productStockService.getStocks(distinctProductIds))
                .willReturn(List.of(firstStock, secondStock));

        List<ProductOrderInfoResponse> responses = productService.getOrderInfos(
                List.of(PRODUCT_ID, PRODUCT_ID_2, PRODUCT_ID)
        );

        assertThat(responses)
                .extracting(ProductOrderInfoResponse::productId)
                .containsExactly(PRODUCT_ID, PRODUCT_ID_2);
        verify(productRepository).findByIdInAndDeletedAtIsNull(distinctProductIds);
        verify(productStockService).getStocks(distinctProductIds);
    }

    @Test
    void 다건_조회에서_상품이_하나라도_없으면_예외가_발생한다() {
        Product product = onSaleProduct(PRODUCT_ID, "사과");
        ProductStock firstStock = ProductStock.create(PRODUCT_ID, 100, 10);
        ProductStock secondStock = ProductStock.create(PRODUCT_ID_2, 50, 5);
        List<UUID> requestedProductIds = List.of(PRODUCT_ID, PRODUCT_ID_2);

        given(productRepository.findByIdInAndDeletedAtIsNull(requestedProductIds))
                .willReturn(List.of(product));
        given(productStockService.getStocks(requestedProductIds))
                .willReturn(List.of(firstStock, secondStock));

        assertBusinessException(
                () -> productService.getOrderInfos(requestedProductIds),
                ErrorCode.PRODUCT_NOT_FOUND
        );
    }

    @Test
    void 다건_조회에서_재고가_하나라도_없으면_예외가_발생한다() {
        Product firstProduct = onSaleProduct(PRODUCT_ID, "사과");
        Product secondProduct = onSaleProduct(PRODUCT_ID_2, "배");
        ProductStock firstStock = ProductStock.create(PRODUCT_ID, 100, 10);
        List<UUID> requestedProductIds = List.of(PRODUCT_ID, PRODUCT_ID_2);

        given(productRepository.findByIdInAndDeletedAtIsNull(requestedProductIds))
                .willReturn(List.of(firstProduct, secondProduct));
        given(productStockService.getStocks(requestedProductIds))
                .willReturn(List.of(firstStock));

        assertBusinessException(
                () -> productService.getOrderInfos(requestedProductIds),
                ErrorCode.PRODUCT_STOCK_NOT_FOUND
        );
    }

    @Test
    void 공개_상품_검색_결과에_이미지_URL을_결합한다() {
        PublicProductSearchCondition condition = publicSearchCondition(ProductStatus.ON_SALE);
        PublicProductQueryResult product = new PublicProductQueryResult(
                PRODUCT_ID, "사과", ProductCategory.FRUIT, 3_000L,
                AppearanceType.NORMAL, "충주", null
        );
        ProductCursorResult<PublicProductQueryResult> expected = ProductCursorResult.of(
                List.of(product), "next-cursor", PRODUCT_ID, true
        );
        given(productQueryRepository.searchPublicProducts(
                condition, null, null, 10, "createdAt", SortDirection.DESC
        )).willReturn(expected);
        given(productImagePort.findImages(List.of(PRODUCT_ID)))
                .willReturn(Map.of(PRODUCT_ID, new ProductImageResult(IMAGE_ID, IMAGE_URL)));

        ProductCursorResult<PublicProductQueryResult> result =
                productService.searchPublicProducts(
                        condition, null, null, 10, "createdAt", SortDirection.DESC
                );

        assertThat(result.content()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(PRODUCT_ID);
            assertThat(item.imageUrl()).isEqualTo(IMAGE_URL);
        });
        assertThat(result.nextCursor()).isEqualTo("next-cursor");
        assertThat(result.nextIdAfter()).isEqualTo(PRODUCT_ID);
        assertThat(result.hasNext()).isTrue();
        verify(productQueryRepository).searchPublicProducts(
                condition, null, null, 10, "createdAt", SortDirection.DESC
        );
        verify(productImagePort).findImages(List.of(PRODUCT_ID));
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
    void 판매자_상품_검색_결과에_이미지_URL을_결합한다() {
        SellerProductSearchCondition condition = new SellerProductSearchCondition(
                "사과",
                ProductCategory.FRUIT,
                ProductStatus.ON_SALE,
                AppearanceType.NORMAL
        );
        Pageable pageable = PageRequest.of(0, 10);
        SellerProductQueryResult product = new SellerProductQueryResult(
                PRODUCT_ID, "사과", ProductCategory.FRUIT, 3_000L,
                ProductStatus.ON_SALE, null
        );
        Page<SellerProductQueryResult> expected =
                new PageImpl<>(List.of(product), pageable, 1);
        given(productQueryRepository.searchSellerProducts(SELLER_ID, condition, pageable))
                .willReturn(expected);
        given(productImagePort.findImages(List.of(PRODUCT_ID)))
                .willReturn(Map.of(PRODUCT_ID, new ProductImageResult(IMAGE_ID, IMAGE_URL)));

        Page<SellerProductQueryResult> result =
                productService.searchSellerProducts(SELLER_ID, SELLER_ROLE, condition, pageable);

        assertThat(result.getContent()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(PRODUCT_ID);
            assertThat(item.imageUrl()).isEqualTo(IMAGE_URL);
        });
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPageable()).isEqualTo(pageable);
        verify(productQueryRepository).searchSellerProducts(SELLER_ID, condition, pageable);
        verify(productImagePort).findImages(List.of(PRODUCT_ID));
        verify(authorizationChecker).requireSeller(SELLER_ROLE);
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
        product.startSale();
        return product;
    }

    private Product onSaleProduct(UUID productId, String name) {
        Product product = Product.create(
                SELLER_ID,
                ProductCategory.FRUIT,
                name,
                "상품 설명",
                3_000L,
                AppearanceType.NORMAL,
                "충주",
                LocalDate.of(2026, 9, 1),
                SaleUnit.KG,
                new BigDecimal("1.00")
        );
        setId(product, productId);
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
