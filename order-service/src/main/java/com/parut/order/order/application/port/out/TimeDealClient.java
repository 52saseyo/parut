package com.parut.order.order.application.port.out;

import java.util.UUID;

import com.parut.order.order.application.port.out.dto.TimeDealInfo;

// Product Service(타임딜 상품, 타임딜 재고) 연동 포트
// ToDo: bulk 도입 시 수정 예정
public interface TimeDealClient {

    TimeDealInfo getOrderInfo(UUID timeDealId);

    void reserveStock(UUID timeDealId, UUID orderId, UUID userId, int quantity);

    void restoreStock(UUID orderId, String reason);
}
