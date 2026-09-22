package com.parut.order.order.application.port.in;

import java.util.UUID;

// 판매자 탈퇴 검증(내부 API)에 쓰이는 미처리 주문 존재 여부 조회 유스케이스.
public interface SellerUnprocessedOrderQueryUseCase {

    boolean hasUnprocessedOrder(UUID sellerId);
}
