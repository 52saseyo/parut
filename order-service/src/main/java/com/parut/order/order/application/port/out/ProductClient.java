package com.parut.order.order.application.port.out;

import java.util.UUID;

import com.parut.order.order.application.port.out.dto.ProductOrderInfo;

// Product Service(일반 상품, 일반 상품 재고) 연동 포트
public interface ProductClient {

    ProductOrderInfo getOrderInfo(UUID productId);

    void reserveStock(UUID productId, UUID orderId, UUID orderItemId, int quantity);

    void restoreStock(UUID productId, UUID orderId, UUID orderItemId);
}
