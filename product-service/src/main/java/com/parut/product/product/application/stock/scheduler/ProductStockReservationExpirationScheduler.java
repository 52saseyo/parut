package com.parut.product.product.application.stock.scheduler;

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

    // 10분마다 만료된 예약을 찾아 자동 복구
    private static final long SCHEDULE_RATE_PROD = 10 * 60 * 1000L;
    // 시연을 위해 스케줄러 실행 주기를 1분으로 단축
    private static final long SCHEDULE_RATE_DEMO = 60 * 1000L;

    private final ProductStockReservationRepository productStockReservationRepository;
    private final ProductStockReservationExpirationProcessor productStockReservationExpirationProcessor;

    @Scheduled(fixedRate = SCHEDULE_RATE_DEMO)
    public void expireReservations() {

        Instant cursorExpiresAt = null;
        UUID cursorId = null;

        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        while(true) {
            List<ProductStockReservation> reservations  = productStockReservationRepository
                    .findNextExpiredBatch(ReservationStatus.RESERVED, Instant.now(), cursorExpiresAt, cursorId, pageable);
            if (reservations.isEmpty()) {
                break;
            }
            log.info("[ExpirationScheduler] {}건의 만료 예약 처리 시작", reservations.size());

            for (ProductStockReservation reservation : reservations) {
                try {
                    // 다른 빈을 통해 호출 -> REQUIRES_NEW 정상 동작, 건마다 독립 트랜잭션
                    productStockReservationExpirationProcessor.expireOneReservation(reservation.getId());
                } catch (BusinessException e) {
                    log.warn("[ExpirationScheduler] 예약 만료 처리 실패: reservationId={}, errorCode={}, message={}",
                            reservation.getId(), e.getErrorCode(), e.getMessage());
                    productStockReservationExpirationProcessor.expirationFailed(reservation.getId());
                } catch (Exception e) {
                    log.error("[ExpirationScheduler] 예상하지 못한 예약 만료 처리 실패: reservationId={}",
                            reservation.getId(), e);
                }
                cursorExpiresAt = reservation.getExpiresAt();
                cursorId = reservation.getId();
            }
        }
        log.info("[ExpirationScheduler] 만료 예약 처리 완료");
    }
}
