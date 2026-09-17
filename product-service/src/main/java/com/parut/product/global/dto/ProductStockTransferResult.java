package com.parut.product.global.dto;

import java.util.UUID;

// NOTE: 일반 상품 재고 이동 결과. quantity는 타임딜 기준 방향으로 되돌려 반환한다.
public record ProductStockTransferResult(
        UUID productId,
        Integer quantity,
        Integer productAvailableQuantity
) {

    public static ProductStockTransferResult of(
            UUID productId,
            Integer quantity,
            Integer productAvailableQuantity
    ) {
        return new ProductStockTransferResult(productId, -quantity, productAvailableQuantity); // NOTE: quantity 부호를 각 서비스마다 반대로 사용하기때문에 -를 붙여 혼동이없도록한다
    }
}
