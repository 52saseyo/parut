package com.parut.product.product.application.stock.service;

import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.product.application.dto.stock.ProductStockItem;
import com.parut.product.product.application.dto.stock.ProductStockReserveItem;
import com.parut.product.product.domain.stock.entity.ProductStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductStockService {
    void createStock(UUID productId, int totalQuantity, int lowStockThreshold);
    ProductStock getStock(UUID productId);
    List<ProductStock> getStocks(List<UUID> productIds);
    void updateStock(UUID productId, UUID requesterId, String requesterRole, int newTotalQuantity);
    void deleteStock(UUID productId, String deletedBy);
    void reserve(UUID orderId, List<ProductStockReserveItem> items);
    void confirm(UUID orderId, List<ProductStockItem> items);
    void restore(UUID orderId, List<ProductStockItem> items);
    Page<ProductStock> getStockList(UUID requesterId, String requesterRole, Pageable pageable);
    ProductStockAllocateResult allocate(ProductStockAllocateCommand command);
    void deallocate(ProductStockAllocateCommand command);
}
