package com.parut.order.order.domain;

import lombok.Getter;

@Getter
public enum CancelReasonCode {
    CUSTOMER_CANCEL(CanceledByType.CUSTOMER),
    SELLER_CANCEL(CanceledByType.SELLER),
    OUT_OF_STOCK(CanceledByType.SYSTEM),
    SYSTEM_TIMEOUT(CanceledByType.SYSTEM);

    private final CanceledByType canceledByType;

    CancelReasonCode(CanceledByType canceledByType) {
        this.canceledByType = canceledByType;
    }

    public boolean matches(CanceledByType canceledByType) {
        return this.canceledByType == canceledByType;
    }
}
