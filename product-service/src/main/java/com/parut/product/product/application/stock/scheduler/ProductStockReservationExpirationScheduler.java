package com.parut.product.product.application.stock.scheduler;

import com.parut.product.global.common.AuditorContext;
import com.parut.product.global.constant.AuditorConstants;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStockReservationExpirationScheduler {

    private static final int BATCH_SIZE = 100;

    private final ProductStockReservationRepository productStockReservationRepository;
    private final ProductStockReservationExpirationProcessor productStockReservationExpirationProcessor;

    @Scheduled(fixedRateString = "${parut.product-stock.scheduler-rate}")
    public void expireReservations() {

        Instant expirationCutoff = Instant.now();
        Instant cursorExpiresAt = null;
        UUID cursorId = null;

        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        while(true) {
            List<ProductStockReservation> reservations = (cursorExpiresAt == null)
                    ? productStockReservationRepository.findFirstExpiredBatch(
                    ReservationStatus.RESERVED,expirationCutoff, pageable)
                    : productStockReservationRepository.findNextExpiredBatchByCursor(
                    ReservationStatus.RESERVED, expirationCutoff, cursorExpiresAt, cursorId, pageable);

            if (reservations.isEmpty()) {
                break;
            }
            log.info("[ExpirationScheduler] {}건의 만료 예약 처리 시작", reservations.size());

            for (ProductStockReservation reservation : reservations) {
                try {
                    // Batch/Scheduler 컨텍스트 - JpaAuditingConfig.auditorProvider()가 HTTP 요청이 없을 때 이 값을 참조한다.
                    AuditorContext.set(AuditorConstants.BATCH_SYSTEM_USER_ID);
                    // 다른 빈을 통해 호출 -> REQUIRES_NEW 정상 동작, 건마다 독립 트랜잭션
                    productStockReservationExpirationProcessor.expireOneReservation(reservation.getId());
                } catch (BusinessException e) {
                    log.warn("[ExpirationScheduler] 예약 만료 처리 실패: reservationId={}, errorCode={}, message={}",
                            reservation.getId(), e.getErrorCode(), e.getMessage());
                    try {
                        productStockReservationExpirationProcessor.expirationFailed(reservation.getId());
                    } catch (Exception ex) {
                        log.error("[ExpirationScheduler] 격리 처리 자체도 실패: reservationId={}", reservation.getId(), ex);
                    }
                } catch (Exception e) {
                    log.error("[ExpirationScheduler] 예상하지 못한 예약 만료 처리 실패: reservationId={}",
                            reservation.getId(), e);
                } finally {
                    AuditorContext.clear();
                }
                cursorExpiresAt = reservation.getExpiresAt();
                cursorId = reservation.getId();
            }
        }
        log.info("[ExpirationScheduler] 만료 예약 처리 완료");
    }
}
