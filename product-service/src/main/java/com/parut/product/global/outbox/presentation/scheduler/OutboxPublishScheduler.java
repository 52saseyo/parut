package com.parut.product.global.outbox.presentation.scheduler;

import com.parut.product.global.outbox.application.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PENDING Outbox 이벤트를 주기적으로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublishScheduler {

    private final OutboxPublisher outboxPublisher;

    @Value("${parut.outbox.publish-batch-size:50}")
    private int publishBatchSize;

    @Scheduled(
            fixedDelayString = "${parut.outbox.publish-interval:5s}",
            scheduler = "outboxPublishTaskScheduler"
    )
    public void publishPendingEvents() {
        int publishedCount = outboxPublisher.publishPendingEvents(publishBatchSize);
        if (publishedCount > 0) {
            log.debug("Outbox 이벤트 발행 완료. publishedCount={}", publishedCount);
        }
    }
}
