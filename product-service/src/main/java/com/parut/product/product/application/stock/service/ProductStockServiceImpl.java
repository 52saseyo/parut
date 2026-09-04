package com.parut.product.product.application.stock.service;


import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.entity.ProductStockEventLog;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.StockEventType;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockEventLogRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductStockServiceImpl implements ProductStockService{

    private final ProductStockRepository productStockRepository;
    private final ProductStockReservationRepository productStockReservationRepository;
    private final ProductStockEventLogRepository productStockEventLogRepository;

    // 상품 등록 시 재고 등록
    @Override
    public void createStock(UUID productId, int totalQuantity, int lowStockThreshold) {
        ProductStock stock = ProductStock.create(productId, totalQuantity, lowStockThreshold);
        productStockRepository.save(stock);
    }

    // 상품 한 개의 재고 조회
    @Override
    @Transactional(readOnly = true)
    public ProductStock getStock(UUID productId) {
        return productStockRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));
    }

    // 상품 여러 개 재고 조회
    @Override
    @Transactional(readOnly = true)
    public List<ProductStock> getStocks(List<UUID> productIds) {
        return productStockRepository.findByProductIdInAndDeletedAtIsNull(productIds);
    }

    // 재고 수정
    @Override
    public void updateStock(UUID productId, int newTotalQuantity) {
        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        int reservedQuantity = stock.getTotalQuantity() - stock.getAvailableQuantity();
        if (newTotalQuantity < reservedQuantity) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_INVALID_QUANTITY);
        }

        int newAvailableQuantity = newTotalQuantity - reservedQuantity;
        stock.adjustQuantity(newTotalQuantity, newAvailableQuantity);
    }

    // 재고 삭제
    @Override
    public void deleteStock(UUID productId, String deletedBy) {
        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));
        stock.softDelete(deletedBy);
    }

    @Override
    public void reserve(UUID productId, UUID orderId, UUID orderItemId, int quantity) {
       if(isAlreadyProcessed(orderItemId, StockEventType.RESERVE)) {
           return;
       }
        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));
        stock.reserve(quantity);
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);

        // 현재 시각 + 30분으로 만료 예약 시간 생성
        ProductStockReservation reservation = ProductStockReservation
                .create(stock.getId(), orderId, quantity, Instant.now().plus(30, ChronoUnit.MINUTES));

        productStockReservationRepository.save(reservation);

        saveEventLog(reservation.getId(), orderItemId, StockEventType.RESERVE);
    }

    @Override
    public void confirm(UUID productId, UUID orderId, UUID orderItemId) {
        if(isAlreadyProcessed(orderItemId, StockEventType.CONFIRM)) {
            return;
        }
        ProductStockReservation reservation = findReservationByOrderItemId(orderItemId, orderId);
        reservation.confirm();

        ProductStock stock = productStockRepository.findById(reservation.getStockId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        validateStockOwnership(stock, productId);

        stock.confirm(reservation.getQuantity());
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);

        saveEventLog(reservation.getId(), orderItemId, StockEventType.CONFIRM);
    }

    @Override
    public void restore(UUID productId, UUID orderId, UUID orderItemId) {
        if (isAlreadyProcessed(orderItemId, StockEventType.RESTORE)) {
            return;
        }

        ProductStockReservation reservation = findReservationByOrderItemId(orderItemId, orderId);
        reservation.cancel();

        ProductStock stock = productStockRepository.findById(reservation.getStockId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        validateStockOwnership(stock, productId);

        stock.restore(reservation.getQuantity());
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);

        saveEventLog(reservation.getId(), orderItemId, StockEventType.RESTORE);
    }

    private boolean isAlreadyProcessed(UUID orderItemId, StockEventType eventType) {
        return productStockEventLogRepository
                .findByOrderItemIdAndEventType(orderItemId, eventType)
                .isPresent();
    }

    // RESERVE 이벤트 로그로 대상 예약을 조회하고, orderId 일치 여부까지 검증
    private ProductStockReservation findReservationByOrderItemId(UUID orderItemId, UUID orderId) {
        ProductStockEventLog reserveLog = productStockEventLogRepository
                .findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND));

        ProductStockReservation reservation = productStockReservationRepository
                .findById(reserveLog.getReservationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND));

        if (!reservation.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }

        return reservation;
    }

    private void saveEventLog(UUID reservationId, UUID orderItemId, StockEventType eventType) {
        ProductStockEventLog eventLog = ProductStockEventLog.create(reservationId, orderItemId, eventType);
        try {
            productStockEventLogRepository.save(eventLog);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 이미 처리됨 -> 멱등처리
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductStock> getStockList(Pageable pageable) {
        return productStockRepository.findByDeletedAtIsNull(pageable);
    }

    // 낙관적 락 검증
    private void saveStockSafely(ProductStock stock, ErrorCode conflictErrorCode) {
        try {
            productStockRepository.saveAndFlush(stock);
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(conflictErrorCode);
        }
    }

    // 재고와 상품이 일치하는지 검증
    private void validateStockOwnership(ProductStock stock, UUID productId) {
        if (!stock.getProductId().equals(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }
    }

}
