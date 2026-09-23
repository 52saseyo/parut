package com.parut.product.global.outbox.infrastructure.persistence;

import com.parut.product.global.outbox.domain.OutboxEvent;
final class OutboxEventMapper {

    private OutboxEventMapper() {
    }

    static OutboxEvent toDomain(JpaOutboxEvent entity) {
        return OutboxEvent.rehydrate(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getAggregateId(),
                entity.getDeduplicationKey(),
                entity.getTraceId(),
                entity.getPayload(),
                entity.getPublishStatus(),
                entity.getRetryCount(),
                entity.getLastError(),
                entity.getPublishedAt(),
                entity.getCreatedAt()
        );
    }
}
