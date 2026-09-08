package com.parut.product.product.application.product.service;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.product.query.ProductQueryRepository;
import com.parut.product.product.application.stock.service.ProductStockService;
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.enums.StockStatus;
import com.parut.product.product.infrastructure.product.persistence.ProductRepository;
import com.parut.product.product.presentation.product.dto.request.CreateProductRequest;
import com.parut.product.product.presentation.product.dto.request.PublicProductSearchCondition;
import com.parut.product.product.presentation.product.dto.request.SellerProductSearchCondition;
import com.parut.product.product.presentation.product.dto.request.UpdateProductRequest;
import com.parut.product.product.presentation.product.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    /**
     * 상품을 생성하고 같은 트랜잭션 안에서 초기 재고를 생성한다.
     */
    @Transactional
    public ProductResponse createProduct(UUID sellerId, CreateProductRequest request) {
        Product product = Product.create(
                sellerId,
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
    public ProductResponse updateProduct(UUID productId, UUID sellerId, UpdateProductRequest request) {
        Product product = productRepository
                .findByIdAndSellerIdAndDeletedAtIsNull(productId, sellerId)
                .orElseThrow( () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

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
    public void deleteProduct(UUID productId, UUID sellerId) {
        Product product = productRepository
                .findByIdAndSellerIdAndDeletedAtIsNull(productId, sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        String deletedBy = sellerId.toString();
        productStockService.deleteStock(productId, deletedBy);
        product.delete(deletedBy);
    }


    /**
     * 판매자 요청에 따라 상품 상태를 변경한다.
     * 직접 변경 가능한 상태는 판매 중과 판매 중지 상태로 제한한다.
     */
    @Transactional
    public ProductResponse updateProductStatus(UUID productId, UUID sellerId, ProductStatus targetStatus) {
        Product product = productRepository
                .findByIdAndSellerIdAndDeletedAtIsNull(
                        productId,
                        sellerId
                ).orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        changeStatus(product, targetStatus);

        return ProductResponse.from(product);
    }

    /**
     * 구매자에게 노출 가능한 공개 상품 상세를 조회한다.
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getProduct(UUID productId) {
        Product product = productRepository
                .findByIdAndStatusInAndDeletedAtIsNull(productId, VISIBLE_STATUSES)
                .orElseThrow(()-> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        return ProductDetailResponse.from(product);
    }

    /**
     * 판매자가 본인 상품 상세를 조회한다.
     * 공개 상품 여부와 무관하게 삭제되지 않은 본인 상품이면 조회할 수 있다.
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getMyProduct(UUID sellerId, UUID productId) {
        Product product = productRepository
                .findByIdAndSellerIdAndDeletedAtIsNull(productId, sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductDetailResponse.from(product);
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




    /**
     * 판매 시작 또는 판매 재개 요청을 처리한다.
     * 판매 가능한 재고가 있는지 확인한 뒤 도메인 상태를 변경한다.
     */
    private void changeToOnSale(Product product) {
        if(product.getStatus() == ProductStatus.DRAFT) {
            validateStockAvailableForSale(product.getId());
            product.startSale();
            return;
        }
        if(product.getStatus() == ProductStatus.SUSPENDED) {
            validateStockAvailableForSale(product.getId());
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
    public Page<PublicProductListResponse> searchPublicProducts(
            PublicProductSearchCondition condition,
            Pageable pageable
    ){
        return productQueryRepository.searchPublicProducts(condition, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SellerProductListResponse> searchSellerProducts(
            UUID sellerId,
            SellerProductSearchCondition condition,
            Pageable pageable
    ){
        return productQueryRepository.searchSellerProducts(
                sellerId,
                condition,
                pageable
        );
    }



}
