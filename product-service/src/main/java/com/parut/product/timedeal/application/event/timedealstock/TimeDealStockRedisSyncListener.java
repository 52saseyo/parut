package com.parut.product.timedeal.application.event.timedealstock;

import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRedisPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealStockRedisSyncListener {

    private final TimeDealStockRedisPort timeDealStockRedisPort;

    /*
     * TODO: Redis 동기화 실패를 반드시 복구해야 하는 단계에서는 여기서 Outbox를 처음 생성하지 않는다.
     * DB 재고 변경과 Redis 동기화 Outbox 저장을 같은 DB 트랜잭션에서 처리하고,
     * Outbox Worker가 재시도·멱등 처리·실패 격리를 담당하도록 확장한다.
     */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TimeDealStockCreatedEvent event) {
        try {
            timeDealStockRedisPort.setAvailableQuantity(
                    event.timeDealId(), event.stockId(), event.availableQuantity());
        } catch (Exception e) {
            // TODO: Outbox 도입 후 재처리 대상 기록·메트릭·알림을 추가한다.
            log.error("[TimeDealRedis] 재고 생성 후 Redis 동기화 실패: timeDealId={}, stockId={}",
                    event.timeDealId(), event.stockId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TimeDealStockAdjustedEvent event) {
        try {
            timeDealStockRedisPort.setAvailableQuantity(
                    event.timeDealId(), event.stockId(), event.availableQuantity());
        } catch (Exception e) {
            // TODO: Outbox 도입 후 재처리 대상 기록·메트릭·알림을 추가한다.
            log.error("[TimeDealRedis] 재고 조정 후 Redis 동기화 실패: timeDealId={}, stockId={}",
                    event.timeDealId(), event.stockId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TimeDealStockTransferredEvent event) {
        try {
            timeDealStockRedisPort.setAvailableQuantity(
                    event.timeDealId(), event.stockId(), event.availableQuantity());
        } catch (Exception e) {
            // TODO: Outbox 도입 후 재처리 대상 기록·메트릭·알림을 추가한다.
            log.error("[TimeDealRedis] 재고 이동 후 Redis 동기화 실패: timeDealId={}, stockId={}",
                    event.timeDealId(), event.stockId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TimeDealStockDeletedEvent event) {
        try {
            timeDealStockRedisPort.delete(event.timeDealId(), event.stockId());
        } catch (Exception e) {
            // TODO: Outbox 도입 후 재처리 대상 기록·메트릭·알림을 추가한다.
            log.error("[TimeDealRedis] 재고 삭제 후 Redis 동기화 실패: timeDealId={}, stockId={}",
                    event.timeDealId(), event.stockId(), e);
        }
    }
}
