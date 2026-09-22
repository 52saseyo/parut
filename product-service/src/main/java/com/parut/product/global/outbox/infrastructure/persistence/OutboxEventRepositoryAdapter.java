package com.parut.product.global.outbox.infrastructure.persistence;

import com.parut.product.global.outbox.application.port.out.OutboxEventRepository;
import com.parut.product.global.outbox.domain.OutboxEvent;
import com.parut.product.global.outbox.domain.OutboxPublishStatus;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final JpaOutboxEventRepository jpaOutboxEventRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        JpaOutboxEvent entity = event.getId() == null
                ? JpaOutboxEvent.from(event)
                : jpaOutboxEventRepository.findById(event.getId())
                .orElseThrow(() -> new IllegalArgumentException("Outbox 이벤트를 찾을 수 없습니다."));
        entity.apply(event);
        return jpaOutboxEventRepository.save(entity).toDomain();
    }

    @Override
    public boolean saveIfAbsent(OutboxEvent event) {
        UUID id = event.getId() != null ? event.getId() : UUID.randomUUID(); // NOTE: kafka event id로 쓰기위해 없으면 랜덤값이라도 추가
        return jpaOutboxEventRepository.insertIfAbsent(
                id,
                event.getEventId(),
                event.getEventType(),
                event.getAggregateId(),
                event.getDeduplicationKey(),
                event.getTraceId(),
                event.getPayload(),
                event.getPublishStatus().name(),
                event.getRetryCount(),
                event.getCreatedAt()
        ) == 1;
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("limit은 1 이상 100 이하여야 합니다.");
        }
        return jpaOutboxEventRepository.findTop100ByPublishStatusOrderByCreatedAtAsc(OutboxPublishStatus.PENDING)
                .stream()
                .limit(limit)
                .map(JpaOutboxEvent::toDomain)
                .toList();
    }
}
