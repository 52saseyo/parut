package com.parut.order.order.application.port.out;

import com.parut.order.order.application.port.out.dto.ProductOrderInfo;
import com.parut.order.order.application.port.out.dto.ProductStockItem;
import com.parut.order.order.application.port.out.dto.ProductStockReserveItem;

import java.util.List;
import java.util.UUID;

// Product Service(일반 상품, 일반 상품 재고) 연동 포트
public interface ProductClient {

    List<ProductOrderInfo> getOrderInfos(List<UUID> productIds);

    void reserveStock(UUID orderId, List<ProductStockReserveItem> items);

    void restoreStock(UUID orderId, List<ProductStockItem> items);
}
