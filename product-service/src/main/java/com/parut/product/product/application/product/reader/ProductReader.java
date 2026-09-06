package com.parut.product.product.application.product.reader;

import java.util.List;
import java.util.UUID;

public interface ProductReader {
    /**
     * 상품 ID로 해당 상품의 판매자 ID를 조회
     */
    UUID getSellerId(UUID productId);

    /**
     * 특정 판매자가 보유한 삭제되지 않은 상품 ID 목록을 조회
     */
    List<UUID> getProductIdsBySellerId(UUID sellerId);

    /**
     * 상품이 특정 판매자의 소유인지 확인
     */
    boolean isOwnedBy(UUID productId, UUID sellerId);
}
