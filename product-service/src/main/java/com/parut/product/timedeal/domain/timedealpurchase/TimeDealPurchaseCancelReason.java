package com.parut.product.timedeal.domain.timedealpurchase;

import lombok.Getter;


@Getter
public enum TimeDealPurchaseCancelReason {

    RESERVATION_EXPIRED("선점 시간 만료로 자동 취소"),
    PAYMENT_FAILED("결제 실패로 자동 취소"),
    ORDER_CANCELED("주문 취소로 인한 취소");

    private final String description;

    TimeDealPurchaseCancelReason(String description) {
        this.description = description;
    }
}