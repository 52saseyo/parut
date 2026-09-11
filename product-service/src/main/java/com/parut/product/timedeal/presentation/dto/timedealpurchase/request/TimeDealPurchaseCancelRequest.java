package com.parut.product.timedeal.presentation.dto.timedealpurchase.request;

import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseCancelCommand;

import java.util.UUID;


public record TimeDealPurchaseCancelRequest(
        String reason
) {

    // NOTE: reason은 null 허용(사유 없는 취소 가능). 길이 제한은 도메인이 지킨다.
    public TimeDealPurchaseCancelCommand toCommand(UUID orderId) {
        return new TimeDealPurchaseCancelCommand(orderId, reason);
    }
}
