package com.parut.product.product.application.stock.service;

import com.parut.product.product.domain.stock.entity.ProductStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductStockService {
    void createStock(UUID productId, int totalQauntity, int lowStockThreshold);
    ProductStock getStock(UUID productId);
    List<ProductStock> getStocks(List<UUID> productIds);
    void updateStock(UUID productId, int totalQuantity);
    void deleteStock(UUID productId, String deletedBy);
    void reserve(UUID productId, UUID orderId, UUID orderItemId, int quantity);
    void confirm(UUID productId, UUID orderId, UUID orderItemId);
    void restore(UUID productId, UUID orderId, UUID orderItemId);
    Page<ProductStock> getStockList(Pageable pageable);
}
