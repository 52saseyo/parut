package com.parut.product.product.application.stock.scheduler;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStockReservationExpirationScheduler {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ITERATIONS = 50;

    // 5분마다 만료된 예약을 찾아 자동 복구
    private static final long SCHEDULE_RATE_PROD = 5 * 60 * 1000L;
    // 시연을 위해 스케줄러 실행 주기를 10초로 단축
    private static final long SCHEDULE_RATE_DEMO = 10 * 1000L;

    private final ProductStockReservationRepository productStockReservationRepository;
    private final ProductStockReservationExpirationProcessor productStockReservationExpirationProcessor;

    @Scheduled(fixedRate = SCHEDULE_RATE_DEMO)
    public void expireReservations() {
        Instant now = Instant.now();// 루프 시작 전 한 번만 고정
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        int iteration = 0;
        while(iteration < MAX_ITERATIONS) {
            Page<ProductStockReservation> page = productStockReservationRepository
                    .findByStatusAndExpiresAtBefore(ReservationStatus.RESERVED, now, pageable);
            if (page.isEmpty()) {
                break;
            }
            log.info("[ExpirationScheduler] {}건의 만료 예약 처리 시작 (iteration={})",
                    page.getNumberOfElements(), iteration);

            for (ProductStockReservation reservation : page.getContent()) {
                try {
                    // 다른 빈을 통해 호출 -> REQUIRES_NEW 정상 동작, 건마다 독립 트랜잭션
                    productStockReservationExpirationProcessor.expireOneReservation(reservation.getId());
                } catch (BusinessException e) {
                    log.warn("[ExpirationScheduler] 예약 만료 처리 실패: reservationId={}, errorCode={}, message={}",
                            reservation.getId(), e.getErrorCode(), e.getMessage());
                } catch (Exception e) {
                    log.error("[ExpirationScheduler] 예상하지 못한 예약 만료 처리 실패: reservationId={}",
                            reservation.getId(), e);
                }
            }
            iteration++;
        }

        if(iteration >= MAX_ITERATIONS) {
            log.warn("[ExpirationScheduler] 최대 반복 횟수({})에 도달하여 중단, 다음 스케줄에서 이어서 처리", MAX_ITERATIONS);
        }
        log.info("[ExpirationScheduler] 만료 예약 처리 완료");
    }
}
