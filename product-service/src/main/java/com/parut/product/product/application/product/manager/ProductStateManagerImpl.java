package com.parut.product.product.application.product.manager;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.infrastructure.product.persistence.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class ProductStateManagerImpl implements ProductStateManager {
    private final ProductRepository productRepository;

    @Override
    public void soldOut(UUID productId) {
        Product product = findProduct(productId);
        product.soldOut();
    }

    @Override
    public void resumeSaleAfterRestock(UUID productId) {
        Product product = findProduct(productId);
        product.resumeSaleAfterRestock();
    }

    private Product findProduct(UUID productId) {
        return productRepository
                .findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
