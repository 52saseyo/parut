package com.parut.order.payment.domain;

public enum PaymentStatus {
    READY,
    IN_PROGRESS,
    DONE,
    PARTIAL_CANCELED,
    CANCELED,
    ABORTED
}
