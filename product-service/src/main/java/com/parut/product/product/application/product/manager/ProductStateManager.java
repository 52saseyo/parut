package com.parut.product.product.application.product.manager;

import java.util.UUID;


public interface ProductStateManager {
    /**
     * 상품을 품절 상태로 변경
     */
    void soldOut(UUID productId);

    /**
     * 재입고된 상품을 판매 상태로 변경
     */
    void resumeSaleAfterRestock(UUID productId);
}
