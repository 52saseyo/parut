package com.parut.product.global.outbox.infrastructure.persistence;

import com.parut.product.global.outbox.domain.OutboxPublishStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaOutboxEventRepository extends JpaRepository<JpaOutboxEvent, UUID> {

    @Modifying
    @Query(value = """
            insert into product_schema.p_outbox_events (
                id,
                event_id,
                event_type,
                aggregate_id,
                deduplication_key,
                trace_id,
                payload,
                publish_status,
                retry_count,
                created_at
            )
            values (
                :id,
                :eventId,
                :eventType,
                :aggregateId,
                :deduplicationKey,
                :traceId,
                cast(:payload as jsonb),
                :publishStatus,
                :retryCount,
                :createdAt
            )
            on conflict (event_type, aggregate_id, deduplication_key)
            do nothing
            """, nativeQuery = true) // NOTE: postgreSQL 전용 문법 on conflict do nothing 사용, 동시에 같은 event 더래도 예외처리하지않기위한 멱등 처리
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType,
            @Param("aggregateId") UUID aggregateId,
            @Param("deduplicationKey") String deduplicationKey,
            @Param("traceId") String traceId,
            @Param("payload") String payload,
            @Param("publishStatus") String publishStatus,
            @Param("retryCount") int retryCount,
            @Param("createdAt") Instant createdAt
    );

    List<JpaOutboxEvent> findTop100ByPublishStatusOrderByCreatedAtAsc(OutboxPublishStatus publishStatus);

    java.util.Optional<JpaOutboxEvent> findByEventId(UUID eventId);
}
