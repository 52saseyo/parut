
package com.parut.product.timedeal.application.event.timedeal;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealScheduleRedisPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealScheduleRedisSyncListener {

    private final TimeDealScheduleRedisPort scheduleRedisPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void scheduleCreated(TimeDealScheduleCreatedEvent event) {
        schedule(event.timeDealId(), event.startAt(), event.endAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void scheduleUpdated(TimeDealScheduleUpdatedEvent event) {
        schedule(event.timeDealId(), event.startAt(), event.endAt());
    }

    private void schedule(UUID timeDealId, Instant startAt, Instant endAt) {
        try {
            scheduleRedisPort.schedule(timeDealId, startAt, endAt);
        } catch (RuntimeException exception) {
            log.error("[TimeDealRedis] 오픈·마감 스케줄 등록 실패. timeDealId={}",
                    timeDealId, exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void remove(TimeDealScheduleDeletedEvent event) {
        try {
            scheduleRedisPort.remove(event.timeDealId());
        } catch (RuntimeException exception) {
            log.error("[TimeDealRedis] 오픈·마감 스케줄 삭제 실패. timeDealId={}",
                    event.timeDealId(), exception);
        }
    }
}
