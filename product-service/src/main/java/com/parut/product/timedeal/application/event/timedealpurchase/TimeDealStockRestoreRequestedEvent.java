package com.parut.product.timedeal.application.event.timedealpurchase;

import java.util.UUID;

public record TimeDealStockRestoreRequestedEvent(UUID eventId) {
    public static final String EVENT_TYPE = "TIME_DEAL_STOCK_RESTORE_REQUESTED";
}
