package com.parut.product.product.application.stock.scheduler;


import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.entity.ProductStockEventLog;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import com.parut.product.product.domain.stock.enums.StockEventType;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockEventLogRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStockReservationExpirationProcessor {

    private final ProductStockReservationRepository productStockReservationRepository;
    private final ProductStockRepository productStockRepository;
    private final ProductStockEventLogRepository productStockEventLogRepository;

    // 예약 건 하나를 독립된 트랜잭션으로 처리 - 반드시 다른 빈을 통해 호출되어야 REQUIRES_NEW가 실제 적용
    // 같은 클래스 내부 호출 시 프록시를 안 거쳐 무시되므로 분리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOneReservation(UUID reservationId) {
        ProductStockReservation reservation = productStockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND));

        ProductStockEventLog reserveLog = productStockEventLogRepository
                .findByReservationIdAndEventType(reservation.getId(), StockEventType.RESERVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND));

        UUID orderItemId = reserveLog.getOrderItemId();

        if(productStockEventLogRepository
                .findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE)
                .isPresent()) {
            return;
        }

        reservation.expire();
        saveReservationSafely(reservation, ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);

        ProductStock stock = productStockRepository.findById(reservation.getStockId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        stock.restore(reservation.getQuantity());
        productStockRepository.saveAndFlush(stock);

        ProductStockEventLog eventLog = ProductStockEventLog.create(reservation.getId(), orderItemId, StockEventType.RESTORE);
        try {
            productStockEventLogRepository.save(eventLog);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }

        log.info("[ExpirationScheduler] 예약 만료 처리 완료: reservationId={}", reservationId);

    }

    private void saveReservationSafely(ProductStockReservation reservation, ErrorCode conflictErrorCode) {
        try {
            productStockReservationRepository.saveAndFlush(reservation);
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(conflictErrorCode);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expirationFailed(UUID reservationId) {
        try {
            productStockReservationRepository.findById(reservationId).ifPresent(reservation -> {
                if (reservation.getStatus() != ReservationStatus.RESERVED) {
                    return;
                }
                reservation.fail();
                productStockReservationRepository.saveAndFlush(reservation);
            });
        } catch (Exception e) {
            log.warn("[ExpirationScheduler] 격리 처리 실패, 스킵: reservationId={}", reservationId, e);
        }
    }
}
