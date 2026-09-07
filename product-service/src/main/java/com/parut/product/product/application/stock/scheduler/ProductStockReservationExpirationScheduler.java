package com.parut.product.product.application.stock.scheduler;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStockReservationExpirationScheduler {

    private static final int BATCH_SIZE = 100;

    private final ProductStockReservationRepository productStockReservationRepository;
    private final ProductStockReservationExpirationProcessor productStockReservationExpirationProcessor;

    // 5분마다 만료된 예약을 찾아 자동 복구
    private static final long SCHEDULE_RATE_PROD = 5 * 60 * 1000L;
    // 시연을 위해 스케줄러 실행 주기를 10초로 단축
    private static final long SCHEDULE_RATE_DEMO = 10 * 1000L;

    @Scheduled(fixedRate = SCHEDULE_RATE_DEMO)
    public void expireReservations() {
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        List<ProductStockReservation> expiredReservations = productStockReservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.RESERVED, Instant.now(), pageable)
                .getContent();
        if(expiredReservations.isEmpty()) {
            return;
        }

        log.info("[ExpirationScheduler] {}건의 만료 예약 처리 시작", expiredReservations.size());

        for(ProductStockReservation reservation : expiredReservations) {
            try{
                // 다른 빈을 통해 호출 -> REQUIRES_NEW 정상 동작, 건마다 독립 트랜잭션
                productStockReservationExpirationProcessor.expireOneReservation(reservation.getId());
            } catch(Exception e) {
                log.error("[ExpirationScheduler] 예약 만료 처리 실패: reservationId={}", reservation.getId(), e);
            }
        }
        log.info("[ExpirationScheduler] 만료 예약 처리 완료");
    }

}
