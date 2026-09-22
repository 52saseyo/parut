package com.parut.order.order.application.port.out;

import com.parut.order.order.application.port.out.dto.TimeDealInfo;

import java.util.UUID;

// Product Service(타임딜 상품, 타임딜 재고) 연동 포트
public interface TimeDealClient {

    TimeDealInfo getOrderInfo(UUID timeDealId);

    void reserveStock(UUID timeDealId, UUID orderId, UUID userId, int quantity);

    void restoreStock(UUID orderId, String reason);
}
