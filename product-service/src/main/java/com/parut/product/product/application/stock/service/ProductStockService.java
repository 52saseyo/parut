package com.parut.product.product.application.stock.service;

import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.product.application.stock.dto.ProductStockHistoryResult;
import com.parut.product.product.application.stock.dto.ProductStockItem;
import com.parut.product.product.application.stock.dto.ProductStockReserveItem;
import com.parut.product.product.application.stock.dto.IsolatedReservationResult;
import com.parut.product.product.domain.stock.entity.ProductStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductStockService {
    void createStock(UUID productId, int totalQuantity, int lowStockThreshold);

    ProductStock getStock(UUID productId);

    List<ProductStock> getStocks(List<UUID> productIds);

    void updateStock(UUID productId, UUID requesterId, String requesterRole, int newTotalQuantity);

    void deleteStock(UUID productId, String deletedBy);

    List<IsolatedReservationResult> getIsolatedReservations(UUID requesterId, String requesterRole);

    void recoverIsolatedReservation(UUID reservationId, UUID requesterId, String requesterRole);

    void reserve(UUID orderId, List<ProductStockReserveItem> items);

    void confirm(UUID orderId, List<ProductStockItem> items);

    void restore(UUID orderId, List<ProductStockItem> items);

    Page<ProductStock> getStockList(UUID requesterId, String requesterRole, Pageable pageable);

    ProductStockAllocateResult allocate(ProductStockAllocateCommand command);

    void deallocate(ProductStockAllocateCommand command);

    ProductStockHistoryResult getStockHistory(UUID productId, UUID requesterId, String requesterRole, String cursor, int size);
}
