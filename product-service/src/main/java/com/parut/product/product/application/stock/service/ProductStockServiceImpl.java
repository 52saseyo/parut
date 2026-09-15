package com.parut.product.product.application.stock.service;


import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.authorization.stock.ProductStockAuthorizationChecker;
import com.parut.product.product.application.dto.stock.ProductStockItem;
import com.parut.product.product.application.dto.stock.ProductStockReserveItem;
import com.parut.product.product.application.product.manager.ProductStateManager;
import com.parut.product.product.application.product.reader.ProductReader;
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.entity.ProductStockEventLog;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import com.parut.product.product.domain.stock.enums.StockEventType;
import com.parut.product.product.domain.stock.enums.StockStatus;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockEventLogRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductStockServiceImpl implements ProductStockService{


    @Value("${parut.product-stock.reservation-ttl}")
    private Duration reservationTtl;


    private final ProductStockRepository productStockRepository;
    private final ProductStockReservationRepository productStockReservationRepository;
    private final ProductStockEventLogRepository productStockEventLogRepository;
    private final ProductReader productReader;
    private final ProductStateManager productStateManager;
    private final ProductStockAuthorizationChecker authorizationChecker;

    // 상품 등록 시 재고 등록
    @Override
    public void createStock(UUID productId, int totalQuantity, int lowStockThreshold) {
        ProductStock stock = ProductStock.create(productId, totalQuantity, lowStockThreshold);

        try {
            productStockRepository.save(stock);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_ALREADY_EXISTS);
        }
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
    public void updateStock(UUID productId, UUID requesterId, String requesterRole, int newTotalQuantity) {
        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        UUID sellerId = productReader.getSellerId(productId);
        authorizationChecker.requireOwnerOrAdmin(requesterId, requesterRole, sellerId);

        StockStatus previousStatus = stock.getStatus();

        int reservedQuantity = stock.getTotalQuantity() - stock.getAvailableQuantity();
        if (newTotalQuantity < reservedQuantity) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_INVALID_QUANTITY);
        }

        int newAvailableQuantity = newTotalQuantity - reservedQuantity;
        stock.adjustQuantity(newTotalQuantity, newAvailableQuantity);
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);

        if (stock.getStatus() == StockStatus.SOLD_OUT) {
            notifySoldOut(productId);
        } else if (previousStatus == StockStatus.SOLD_OUT) {
            notifyRestocked(productId);
        }

    }

    // 재고 삭제
    @Override
    public void deleteStock(UUID productId, String deletedBy) {
        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));
        stock.softDelete(deletedBy);
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);
    }

    @Override
    public void reserve(UUID orderId, List<ProductStockReserveItem> items) {
        Instant expiresAt = Instant.now().plus(reservationTtl);
        for (ProductStockReserveItem item : items) {
            if (isAlreadyProcessed(item.orderItemId(), StockEventType.RESERVE)) {
                continue;
            }
            ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(item.productId())
                    .orElseThrow(() -> {
                        log.warn("[reserve] 재고 없음 - orderId={}, orderItemId={}, productId={}",
                                orderId, item.orderItemId(), item.productId());
                        return new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND);
                    });
            stock.reserve(item.quantity());

            try {
                productStockRepository.saveAndFlush(stock);
            } catch (OptimisticLockingFailureException e) {
                if (isAlreadyProcessed(item.orderItemId(), StockEventType.RESERVE)) {
                    continue;
                }
                log.warn("[reserve] 낙관적 락 충돌 - orderId={}, orderItemId={}, productId={}",
                        orderId, item.orderItemId(), item.productId());
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_CONFLICT);
            }
            ProductStockReservation reservation = ProductStockReservation
                    .create(stock.getId(), orderId, item.quantity(), expiresAt);
            productStockReservationRepository.save(reservation);

            saveEventLog(reservation.getId(), item.orderItemId(), StockEventType.RESERVE);
        }
    }
    @Override
    public void confirm(UUID orderId, List<ProductStockItem> items) {
        for (ProductStockItem item : items) {
            if (isAlreadyProcessed(item.orderItemId(), StockEventType.CONFIRM)) {
                continue;
            }
            ProductStockReservation reservation = findReservationByOrderItemId(item.orderItemId(), orderId);
            reservation.confirm();
            saveReservationSafely(reservation, ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
            ProductStock stock = productStockRepository.findById(reservation.getStockId())
                    .orElseThrow(() -> {
                        log.warn("[confirm] 재고 없음 - orderId={}, orderItemId={}, stockId={}",
                                orderId, item.orderItemId(), reservation.getStockId());
                        return new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND);
                    });
            validateStockOwnership(stock, item.productId());
            stock.confirm(reservation.getQuantity());
            saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
            if (stock.getStatus() == StockStatus.SOLD_OUT) {
                notifySoldOut(item.productId());
            }
            saveEventLog(reservation.getId(), item.orderItemId(), StockEventType.CONFIRM);
        }
    }

    @Override
    public void restore(UUID orderId, List<ProductStockItem> items) {
        for (ProductStockItem item : items) {
            if (isAlreadyProcessed(item.orderItemId(), StockEventType.RESTORE)) {
                continue;
            }
            ProductStockReservation reservation = findReservationByOrderItemId(item.orderItemId(), orderId);

            if (reservation.getStatus() == ReservationStatus.EXPIRED) {
                // 스케줄러가 이미 만료 처리(재고 복구 + RESTORE 이벤트로그 저장)까지 원자적으로 끝냄
                // -> 재고/로그 재처리 없이 멱등 반환
                continue;
            }

            if (reservation.getStatus() == ReservationStatus.EXPIRATION_FAILED) {
                // 재고 복구 여부가 불확실한 격리 상태
                log.warn("[restore] 격리된 예약 - orderId={}, orderItemId={}, reservationId={}",
                        orderId, item.orderItemId(), reservation.getId());
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ISOLATED);
            }

            reservation.cancel();
            saveReservationSafely(reservation, ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);

            ProductStock stock = productStockRepository.findById(reservation.getStockId())
                    .orElseThrow(() -> {
                        log.warn("[restore] 재고 없음 - orderId={}, orderItemId={}, stockId={}",
                                orderId, item.orderItemId(), reservation.getStockId());
                        return new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND);
                    });

            validateStockOwnership(stock, item.productId());

            stock.restore(reservation.getQuantity());
            saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);

            saveEventLog(reservation.getId(), item.orderItemId(), StockEventType.RESTORE);
        }
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
    public Page<ProductStock> getStockList(UUID requesterId, String requesterRole, Pageable pageable) {
        if (authorizationChecker.isAdmin(requesterRole)) {
            return productStockRepository.findByDeletedAtIsNull(pageable);
        }
        List<UUID> productIds = productReader.getProductIdsBySellerId(requesterId);
        return productStockRepository.findByProductIdInAndDeletedAtIsNull(productIds, pageable);
    }

    // 타임딜 전환 메서드
    @Override
    public ProductStockAllocateResult allocate(ProductStockAllocateCommand command) {
        Product product = productReader.getProduct(command.productId());
        authorizationChecker.requireOwnerOrAdmin(command.requesterId(), command.requesterRole(), product.getSellerId());

        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_PRODUCT_NOT_ON_SALE);
        }

        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(command.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        stock.allocate(command.quantity());
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);
        if (stock.getStatus() == StockStatus.SOLD_OUT) {
            notifySoldOut(command.productId());
        }
        return new ProductStockAllocateResult(
                product.getId(),
                product.getSellerId(),
                product.getImageId(),
                command.quantity(),
                product.getName(),
                product.getDescription(),
                product.getAppearanceType().name(),
                product.getOrigin(),
                product.getHarvestDate(),
                product.getPrice()
        );
    }

    // 재고 복구 메서드
    @Override
    public void deallocate(ProductStockAllocateCommand command) {
        UUID sellerId = productReader.getSellerId(command.productId());
        authorizationChecker.requireOwnerOrAdmin(command.requesterId(), command.requesterRole(), sellerId);

        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(command.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        StockStatus previousStatus = stock.getStatus();

        stock.deallocate(command.quantity());
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);
        if (previousStatus == StockStatus.SOLD_OUT && stock.getStatus() != StockStatus.SOLD_OUT) {
            notifyRestocked(command.productId());
        }
    }

    // 낙관적 락 검증 (재고)
    private void saveStockSafely(ProductStock stock, ErrorCode conflictErrorCode) {
        try {
            productStockRepository.saveAndFlush(stock);
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(conflictErrorCode);
        }
    }

    // 낙관적 락 검증 (재고예약)
    private void saveReservationSafely(ProductStockReservation reservation, ErrorCode conflictErrorCode) {
        try {
            productStockReservationRepository.saveAndFlush(reservation);
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

    // 품절 알림 메서드
    private void notifySoldOut(UUID productId) {
        try {
            productStateManager.soldOut(productId);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED) {
                throw e;
            }
            // 이미 SOLD_OUT 상태라 전이 불가 -> 멱등 처리, 무시
        }
    }

    private void notifyRestocked(UUID productId) {
        try {
            productStateManager.resumeSaleAfterRestock(productId);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED
                    && e.getErrorCode() != ErrorCode.PRODUCT_IMAGE_REQUIRED) {
                throw e;
            }
        }
    }
}
