package com.parut.product.global.outbox.infrastructure.persistence;

import com.parut.product.global.outbox.domain.OutboxPublishStatus;
import com.parut.product.global.outbox.domain.OutboxEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "p_outbox_events", schema = "product_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JpaOutboxEvent {

    // NOTE: Outbox 테이블의 한 행을 식별하는 DB 내부 ID다. 현재는 eventId와 분리한다.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    // NOTE: Kafka로 전달되는 이벤트의 식별자다. 재시도 시에도 같은 값을 유지해 소비자가 중복을 판별한다.
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    // NOTE: 이벤트의 종류다. 예: TIME_DEAL_OPENING_SOON, PRODUCT_STOCK_CHANGED
    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    // NOTE: 이벤트가 발생한 도메인 객체의 ID다. 타임딜 알림에서는 timeDealId가 들어간다.
    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    // NOTE: 같은 도메인 객체에서 같은 이벤트를 한 번만 만들기 위한 업무 중복 방지 값이다.
    //       타임딜 시작 임박 이벤트에서는 timeDealStartAt 문자열이 들어간다.
    @Column(name = "deduplication_key", nullable = false, updatable = false, length = 200)
    private String deduplicationKey;

    // NOTE: Kafka Header로 전달할 추적 ID다. HTTP 요청이 없으면 스케줄러가 새 UUID를 생성한다.
    @Column(name = "trace_id", nullable = false, updatable = false, length = 100)
    private String traceId;

    // NOTE: Kafka message value로 발행할 이벤트 본문(JSON)이다. 재시도 시에도 이 값을 그대로 사용한다.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb", updatable = false)
    private String payload;

    // NOTE: Kafka 발행 상태다. PENDING은 대기, PUBLISHED는 발행 완료, FAILED는 재시도 한도 초과 상태다.
    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false, length = 20)
    private OutboxPublishStatus publishStatus;

    // NOTE: Kafka 발행 실패 후 재시도한 횟수다.
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    // NOTE: 가장 최근 Kafka 발행 실패 원인이다. 아직 실패하지 않았으면 null이다.
    @Column(name = "last_error")
    private String lastError;

    // NOTE: Kafka 발행이 성공한 시각이다. PENDING 또는 FAILED 상태에서는 null이다.
    @Column(name = "published_at")
    private Instant publishedAt;

    // NOTE: Outbox 이벤트가 DB에 처음 저장된 시각이다. Publisher 처리 순서와 발행 지연 측정에 사용한다.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static JpaOutboxEvent from(OutboxEvent event) {
        JpaOutboxEvent entity = new JpaOutboxEvent();
        entity.eventId = event.getEventId();
        entity.eventType = event.getEventType();
        entity.aggregateId = event.getAggregateId();
        entity.deduplicationKey = event.getDeduplicationKey();
        entity.traceId = event.getTraceId();
        entity.payload = event.getPayload();
        entity.publishStatus = event.getPublishStatus();
        entity.retryCount = event.getRetryCount();
        entity.lastError = event.getLastError();
        entity.publishedAt = event.getPublishedAt();
        entity.createdAt = event.getCreatedAt();
        return entity;
    }

    public void apply(OutboxEvent event) {
        this.publishStatus = event.getPublishStatus();
        this.retryCount = event.getRetryCount();
        this.lastError = event.getLastError();
        this.publishedAt = event.getPublishedAt();
    }

    public OutboxEvent toDomain() {
        return OutboxEventMapper.toDomain(this);
    }
}
