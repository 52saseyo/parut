package com.parut.order.payment.application.port.out;

import java.util.UUID;

// Product Service(타임딜 재고 확정) 연동 포트
// ToDo: bulk 도입 시 수정 예정
public interface TimeDealStockConfirmClient {

    void confirmStock(UUID orderId);
}
