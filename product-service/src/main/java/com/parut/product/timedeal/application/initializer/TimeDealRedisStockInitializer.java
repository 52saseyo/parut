package com.parut.product.timedeal.application.initializer;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRedisPort;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * SQL fixture 입력이나 Redis 재시작으로 사라진 stock Key를 복구한다.
 * 이미 존재하는 Key는 덮어쓰지 않아 진행 중인 선점을 초기화하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealRedisStockInitializer {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealStockRepository timeDealStockRepository;
    private final TimeDealStockRedisPort timeDealStockRedisPort;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeAfterApplicationReady() {
        reconcile();
    }

    public void reconcile() {
        Instant now = Instant.now();
        try {
            timeDealRepository.findTimeDealsAvailableForRedis(now).forEach(timeDealId ->
                    timeDealStockRepository.findByTimeDealId(timeDealId).ifPresent(this::initializeIfAbsent)
            );
        } catch (RuntimeException e) {
            // Redis·DB 일시 장애가 애플리케이션 기동 자체를 중단시키지 않도록 기록한다.
            // 운영용 주기 재시도는 별도 복구 스케줄러 단계에서 추가한다.
            log.error("[TimeDealRedis] 서버 시작 시 stock Key 초기화 실패.", e);
        }
    }

    private void initializeIfAbsent(TimeDealStock stock) {
        boolean initialized = timeDealStockRedisPort.setAvailableQuantityIfAbsent(
                stock.getTimeDealId(), stock.getId(), stock.getAvailableQuantity());
        if (initialized) {
            log.info(
                    "[TimeDealRedis] 누락된 stock Key를 DB 기준으로 초기화. timeDealId={}, stockId={}, availableQuantity={}",
                    stock.getTimeDealId(), stock.getId(), stock.getAvailableQuantity()
            );
        }
    }
}
