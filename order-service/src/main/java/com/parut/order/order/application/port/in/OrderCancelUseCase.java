package com.parut.order.order.application.port.in;

import java.util.UUID;

// Order가 제공하는 주문 취소 포트 (결제 도메인이 사용)
public interface OrderCancelUseCase {

    // 재고 확정 실패(품절) 시 시스템이 주문 전체를 취소한다. MVP는 단일 상품만 지원
    void cancelForStockShortage(UUID orderId);
}
