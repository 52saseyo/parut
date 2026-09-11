package com.parut.order.payment.application.port.out;

import java.util.UUID;

// Product Service(일반 상품 재고 확정) 연동 포트
// ToDo: bulk 도입 시 수정 예정
public interface ProductStockConfirmClient {

    void confirmStock(UUID productId, UUID orderId, UUID orderItemId);
}
