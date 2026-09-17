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
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.enums.StockStatus;
import com.parut.product.product.infrastructure.product.persistence.ProductRepository;
import com.parut.product.product.presentation.product.dto.request.CreateProductRequest;
import com.parut.product.product.presentation.product.dto.request.UpdateProductRequest;
import com.parut.product.product.presentation.product.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final List<ProductStatus> VISIBLE_STATUSES =
            List.of(
                    ProductStatus.ON_SALE,
                    ProductStatus.SOLD_OUT
            );

    private final ProductRepository productRepository;
    private final ProductStockService productStockService;
    private final ProductQueryRepository productQueryRepository;
    private final ProductImagePort productImagePort;
    private final ProductAuthorizationChecker authorizationChecker;

    /**
     * 상품을 생성하고 같은 트랜잭션 안에서 초기 재고를 생성한다.
     */
    @Transactional
    public ProductResponse createProduct(
            UUID requesterId,
            String requesterRole,
            CreateProductRequest request
    ) {
        authorizationChecker.requireSeller(requesterRole);

        Product product = Product.create(
                requesterId,
                request.category(),
                request.name(),
                request.description(),
                request.price(),
                request.appearanceType(),
                request.origin(),
                request.harvestDate(),
                request.saleUnit(),
                request.unitQuantity()
        );

        Product savedProduct = productRepository.save(product);

        productStockService.createStock(
                savedProduct.getId(),
                request.totalQuantity(),
                request.lowStockThreshold()
        );
        return ProductResponse.from(savedProduct);
    }



    /**
     * 판매자 소유 상품의 기본 정보를 수정한다.
     */
    @Transactional
    public ProductResponse updateProduct(
            UUID productId,
            UUID requesterId,
            String requesterRole,
            UpdateProductRequest request
    ) {
        Product product = findProduct(productId);

        authorizationChecker.requireSellerOwner(
                requesterId,
                requesterRole,
                product.getSellerId()
        );
        product.update(
                request.category(),
                request.name(),
                request.description(),
                request.price(),
                request.appearanceType(),
                request.origin(),
                request.harvestDate(),
                request.saleUnit(),
                request.unitQuantity()
        );
        return ProductResponse.from(product);
    }

    /**
     * 판매자 소유 상품과 연결된 재고, 이미지를 소프트 삭제한다.
     */
    @Transactional
    public void deleteProduct(
            UUID productId,
            UUID requesterId,
            String requesterRole
    ) {
        Product product = findProduct(productId);

        authorizationChecker.requireSellerOwner(
                requesterId,
                requesterRole,
                product.getSellerId()
        );

        String deletedBy = requesterId.toString();
        productStockService.deleteStock(productId, deletedBy);
        product.delete(deletedBy);
    }


    /**
     * 판매자 요청에 따라 상품 상태를 변경한다.
     * 직접 변경 가능한 상태는 판매 중과 판매 중지 상태로 제한한다.
     */
    @Transactional
    public ProductResponse updateProductStatus(
            UUID productId,
            UUID requesterId,
            String requesterRole,
            ProductStatus targetStatus
    ) {
        Product product = findProduct(productId);

        authorizationChecker.requireSellerOwner(
                requesterId,
                requesterRole,
                product.getSellerId()
        );

        changeStatus(product, targetStatus);

        return ProductResponse.from(product);
    }

    /**
     * 구매자에게 노출 가능한 공개 상품 상세를 조회한다.
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getProduct(UUID productId) {
        Product product = findProduct(productId);

        ProductImageResult image = productImagePort.findImage(productId)
                .orElseThrow(()-> new BusinessException(ErrorCode.PRODUCT_IMAGE_REQUIRED));

        String imageUrl = image.imageUrl();

        return ProductDetailResponse.from(product, imageUrl);
    }

    /**
     * 판매자가 본인 상품 상세를 조회한다.
     * 공개 상품 여부와 무관하게 삭제되지 않은 본인 상품이면 조회할 수 있다.
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getMyProduct(
            UUID productId,
            UUID requesterId,
            String requesterRole
    ) {
        Product product = findProduct(productId);

        authorizationChecker.requireSellerOwner(
                requesterId,
                requesterRole,
                product.getSellerId()
        );

        ProductImageResult image = productImagePort.findImage(productId)
                .orElse(null);

        if ((product.getStatus() == ProductStatus.ON_SALE
                || product.getStatus() == ProductStatus.SOLD_OUT)
                && image == null) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_REQUIRED);
        }

        return ProductDetailResponse.from(product, image == null ? null : image.imageUrl());
    }



    /**
     * API에서 허용하는 상품 상태 변경 요청을 실제 도메인 메서드로 위임한다.
     */
    private void changeStatus(Product product, ProductStatus targetStatus) {
        switch (targetStatus) {
            case ON_SALE -> changeToOnSale(product);
            case SUSPENDED -> product.suspend();
            // stock에서 상품의 SOLD_OUT을 처리
            case DRAFT, SOLD_OUT, DELETED -> throw new BusinessException(ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED);
        }
    }



    /**
     * 주문 서비스에서 사용할 상품 주문 정보를 조회한다.
     * 상품 상태와 재고 수량을 함께 확인해 구매 가능 여부를 계산한다.
     */
    @Transactional(readOnly = true)
    public ProductOrderInfoResponse getOrderInfo(UUID productId) {
        Product product = productRepository
                .findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductStock productStock = productStockService.getStock(productId);

        boolean purchasable =
                product.getStatus() == ProductStatus.ON_SALE && productStock.getAvailableQuantity() > 0;

        return ProductOrderInfoResponse.from(product, productStock, purchasable);
    }


    @Transactional
    public void registerImage(
            UUID productId,
            UUID requesterId,
            String requesterRole,
            UUID imageId
    ) {
        Product product = findProduct(productId);

        authorizationChecker.requireSellerOwner(
                requesterId,
                requesterRole,
                product.getSellerId()
        );

        productImagePort.save(requesterId, productId, imageId);
    }



    private void validateImageForSale(UUID productId){
        if(!productImagePort.hasImage(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_REQUIRED);
        }
    }
    /**
     * 판매 시작 또는 판매 재개 요청을 처리한다.
     * 판매 가능한 재고가 있는지 확인한 뒤 도메인 상태를 변경한다.
     */
    private void changeToOnSale(Product product) {
        if(product.getStatus() == ProductStatus.DRAFT) {
            validateStockAvailableForSale(product.getId());
            validateImageForSale(product.getId());
            product.startSale();
            return;
        }
        if(product.getStatus() == ProductStatus.SUSPENDED) {
            validateStockAvailableForSale(product.getId());
            validateImageForSale(product.getId());
            product.resumeSale();
            return;
        }
        throw new BusinessException(ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED);
    }

    /**
     * 상품을 판매 상태로 전환하기 전에 재고가 판매 가능한 상태인지 검증한다.
     */
    private void validateStockAvailableForSale(UUID productId) {
        ProductStock stock = productStockService.getStock(productId);

        if(stock.getStatus() == StockStatus.SOLD_OUT || stock.getAvailableQuantity() <= 0){
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_PRODUCT_NOT_ON_SALE);
        }
    }

    @Transactional(readOnly = true)
    public ProductCursorResult<PublicProductQueryResult> searchPublicProducts(
            PublicProductSearchCondition condition,
            String cursor,
            UUID cursorId,
            int size,
            String sort,
            SortDirection direction
    ){
        validatePublicStatus(condition.status());

        return productQueryRepository.searchPublicProducts(
                condition,
                cursor,
                cursorId,
                size,
                sort,
                direction
        );
    }

    @Transactional(readOnly = true)
    public Page<SellerProductQueryResult> searchSellerProducts(
            UUID sellerId,
            String requesterRole,
            SellerProductSearchCondition condition,
            Pageable pageable
    ){
        authorizationChecker.requireSeller(requesterRole);
        return productQueryRepository.searchSellerProducts(
                sellerId,
                condition,
                pageable
        );
    }

    private void validatePublicStatus(ProductStatus status) {
        if (status == null) {
            return;
        }

        if (status != ProductStatus.ON_SALE && status != ProductStatus.SOLD_OUT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 주문 서비스에서 요청한 여러 상품의 주문 정보를 한 번에 조회한다.
     *
     * 상품과 재고를 각각 다건 조회한 뒤 productId를 기준으로 조합한다.
     * Repository의 IN 조회 결과는 요청 순서를 보장하지 않으므로,
     * 최종 응답은 요청된 productId 순서대로 생성한다.
     *
     * @param productIds 주문 정보를 조회할 상품 ID 목록
     * @return 요청 순서대로 정렬된 상품 주문 정보
     * @throws BusinessException 상품 또는 재고 정보가 하나라도 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public List<ProductOrderInfoResponse> getOrderInfos(List<UUID> productIds) {
        List<UUID> distinctProductIds = List.copyOf(new LinkedHashSet<>(productIds));
        List<Product> products = productRepository.findByIdInAndDeletedAtIsNull(distinctProductIds);

        Map<UUID, Product> productById = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> p
                ));

        List<ProductStock> stocks = productStockService.getStocks(distinctProductIds);

        Map<UUID, ProductStock> stockByProductId = stocks.stream()
                .collect(Collectors.toMap(
                        ProductStock::getProductId,
                        stock -> stock
                ));


        validateAllProductsFound(distinctProductIds, productById, stockByProductId);

        return distinctProductIds.stream()
                .map(productId -> {
                    Product product = productById.get(productId);
                    ProductStock stock = stockByProductId.get(productId);

                    boolean purchasable =
                            product.getStatus() == ProductStatus.ON_SALE
                                    && stock.getAvailableQuantity() > 0;
                    return ProductOrderInfoResponse.from(product, stock , purchasable);
                })
                .toList();

    }


    /**
     * 요청한 모든 상품에 상품 정보와 재고 정보가 존재하는지 검증
     */
    private void validateAllProductsFound(
            List<UUID> requestedProductIds,
            Map<UUID, Product> productById,
            Map<UUID, ProductStock> stockByProductId
    ) {
        for(UUID productId : requestedProductIds) {
            if(!productById.containsKey(productId)) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if(!stockByProductId.containsKey(productId)) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND);
            }

        }
    }

    /**
     * 삭제되지 않은 상품을 조회한다.
     *
     * @throws BusinessException 상품이 존재하지 않는 경우
     */
    private Product findProduct(UUID productId) {
        return productRepository
                .findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.PRODUCT_NOT_FOUND
                        )
                );
    }


}
