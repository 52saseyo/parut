package com.parut.product.timedeal.application.port.out.timedealstock;

public enum TimeDealStockReservationResult {
    RESERVED,
    SOLD_OUT,
    DUPLICATE_ORDER,
    INVALID_QUANTITY,
    STOCK_NOT_INITIALIZED,
    INSUFFICIENT_STOCK
}
