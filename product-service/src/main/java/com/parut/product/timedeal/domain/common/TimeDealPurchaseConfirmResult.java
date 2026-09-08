package com.parut.product.timedeal.domain.common;

// NOTE: 판매 확정 결과. 만료 실패를 예외로 던지면 만료 정리까지 롤백되므로 반환값으로 표현한다.
// NOTE: TimeDealPurchaseStatus를 쓰지 않는 이유는 RESERVED를 반환하는 경우가 없어 계약이 넓어지기 때문이다.
public enum TimeDealPurchaseConfirmResult {

    CONFIRMED,

    CANCELLED
}
