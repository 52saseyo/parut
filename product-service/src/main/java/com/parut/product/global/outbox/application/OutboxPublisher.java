package com.parut.product.global.outbox.application;

import com.parut.product.global.outbox.application.port.out.OutboxEventRepository;
import com.parut.product.global.outbox.application.port.out.OutboxMessagePublisher;
import com.parut.product.global.outbox.domain.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import com.parut.product.global.outbox.domain.OutboxPublishStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Publisher의 애플리케이션 진입점이다.
 *
 * PENDING Outbox 이벤트를 조회하고 메시지 발행 및 상태 변경을 조율한다.
 */
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int MAX_RETRY_COUNT = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxMessagePublisher outboxMessagePublisher;

    @Transactional(readOnly = true)
    public List<OutboxEvent> loadPendingEvents(int batchSize) {
        return outboxEventRepository.findPending(batchSize);
    }

    /**
     * PENDING 이벤트를 배치로 발행한다.
     * 이벤트 처리 성공 후에만 PUBLISHED로 변경하고, 실패한 이벤트는 다음 실행에서 재시도한다.
     */
    @Transactional
    public int publishPendingEvents(int batchSize) {
        List<OutboxEvent> pendingEvents = loadPendingEvents(batchSize); // NOTE: 프록시를 거치지않아도 문제없어서 trancational 일단 그대로 사용
        int publishedCount = 0;

        for (OutboxEvent event : pendingEvents) {
            try {
                outboxMessagePublisher.publish(event);
                event.markPublished(Instant.now());
                outboxEventRepository.save(event);
                publishedCount++;
            } catch (Exception exception) {
                markFailure(event, resolveErrorMessage(exception));
                outboxEventRepository.save(event);
            }
        }

        return publishedCount;
    }

    @Transactional
    public void publishOne(UUID eventId) {
        Optional<OutboxEvent> event = outboxEventRepository.findByEventId(eventId);
        if (event.isEmpty() || event.get().getPublishStatus() == OutboxPublishStatus.PUBLISHED) {
            return;
        }
        OutboxEvent pendingEvent = event.get();
        try {
            outboxMessagePublisher.publish(pendingEvent);
            pendingEvent.markPublished(Instant.now());
            outboxEventRepository.save(pendingEvent);
        } catch (Exception exception) {
            markFailure(pendingEvent, resolveErrorMessage(exception));
            outboxEventRepository.save(pendingEvent);
            throw new IllegalStateException("Outbox 이벤트 처리에 실패했습니다. eventId=" + eventId, exception);
        }
    }

    private void markFailure(OutboxEvent event, String errorMessage) {
        if (event.getRetryCount() + 1 >= MAX_RETRY_COUNT) {
            event.markFailed(errorMessage);
            return;
        }
        event.markRetryableFailure(errorMessage);
    }

    private String resolveErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 1_000 ? message.substring(0, 1_000) : message;
    }
}
