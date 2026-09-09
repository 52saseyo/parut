package com.parut.product.product.application.product.reader;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.infrastructure.product.persistence.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReaderImpl implements ProductReader {
    private final ProductRepository productRepository;

    /**
     * 삭제되지 않은 상품의 판매자 ID를 반환
     * 상품이 없거나 삭제된 경우 상품 없음 예외를 발생
     */
    @Override
    public UUID getSellerId(UUID productId) {
        return productRepository
                .findSellerIdByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 판매자가 소유한 삭제되지 않은 상품 ID 목록을 반환
     */
    @Override
    public List<UUID> getProductIdsBySellerId(UUID sellerId) {
        return productRepository.findProductIdsBySellerId(sellerId);
    }

    /**
     * 상품 ID와 판매자 ID가 일치하는 삭제되지 않은 상품이 있는지 확인
     */
    @Override
    public boolean isOwnedBy(UUID productId, UUID sellerId) {
        return productRepository.existsByIdAndSellerIdAndDeletedAtIsNull(productId, sellerId);
    }
}
