package com.parut.order.payment.application.port.out;

import com.parut.order.payment.application.port.out.dto.ProductStockConfirmItem;

import java.util.List;
import java.util.UUID;

// Product Service(일반 상품 재고 확정) 연동 포트
public interface ProductStockConfirmClient {

    void confirmStock(UUID orderId, List<ProductStockConfirmItem> items);
}
