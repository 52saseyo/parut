package com.parut.product.timedeal.application.event.timedealpurchase;

import com.parut.product.global.outbox.application.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealPurchaseReservationReleaseListener {
    private final OutboxPublisher outboxPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void release(TimeDealPurchaseReservationReleasedEvent event) {
        try {
            outboxPublisher.publishOne(event.eventId());
        } catch (RuntimeException exception) {
            log.error("Redis 재고 복구 실패, Outbox 스케줄러가 재시도합니다. eventId={}", event.eventId(), exception);
        }
    }
}
