package com.parut.product.global.outbox.infrastructure.persistence;

import com.parut.product.global.outbox.application.port.out.OutboxEventRepository;
import com.parut.product.global.outbox.domain.OutboxEvent;
import com.parut.product.global.outbox.domain.OutboxPublishStatus;
import java.util.List;
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
