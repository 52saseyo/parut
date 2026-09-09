package com.parut.product.global.dto;

import java.util.UUID;


// NOTE: 할당 결과. price와 sellerId는 product가 판정한 최종 책임 값이므로 timedeal은 그것을 믿고
// TimeDeal의 originalPrice와 판매자로 쓴다 — 요청에서 받은 값을 쓰면 원가,소유자 위조가 가능하다.
public record ProductStockAllocateResult(
        UUID productId,
        Integer quantity,
        UUID sellerId,
        Long price
) {
}