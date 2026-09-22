package com.parut.product.global.outbox.domain;

import java.time.Instant;
import java.util.UUID;

public class OutboxEvent {

    private final UUID id;
    private final UUID eventId;
    private final String eventType;
    private final UUID aggregateId;
    private final String deduplicationKey;
    private final String traceId;
    private final String payload;
    private OutboxPublishStatus publishStatus;
    private int retryCount;
    private String lastError;
    private Instant publishedAt;
    private final Instant createdAt;

    private OutboxEvent(
            UUID id,
            UUID eventId,
            String eventType,
            UUID aggregateId,
            String deduplicationKey,
            String traceId,
            String payload,
            OutboxPublishStatus publishStatus,
            int retryCount,
            String lastError,
            Instant publishedAt,
            Instant createdAt
    ) {
        this.id = id;
        this.eventId = require(eventId, "eventId");
        this.eventType = requireText(eventType, "eventType");
        this.aggregateId = require(aggregateId, "aggregateId");
        this.deduplicationKey = requireText(deduplicationKey, "deduplicationKey");
        this.traceId = requireText(traceId, "traceId");
        this.payload = requireText(payload, "payload");
        this.publishStatus = publishStatus;
        this.retryCount = retryCount;
        this.lastError = lastError;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
    }

    public static OutboxEvent pending(
            UUID eventId,
            String eventType,
            UUID aggregateId,
            String deduplicationKey,
            String traceId,
            String payload,
            Instant createdAt
    ) {
        return new OutboxEvent(
                null,
                eventId,
                eventType,
                aggregateId,
                deduplicationKey,
                traceId,
                payload,
                OutboxPublishStatus.PENDING,
                0,
                null,
                null,
                require(createdAt, "createdAt")
        );
    }

    public static OutboxEvent rehydrate(
            UUID id,
            UUID eventId,
            String eventType,
            UUID aggregateId,
            String deduplicationKey,
            String traceId,
            String payload,
            OutboxPublishStatus publishStatus,
            int retryCount,
            String lastError,
            Instant publishedAt,
            Instant createdAt
    ) {
        return new OutboxEvent(
                require(id, "id"),
                eventId,
                eventType,
                aggregateId,
                deduplicationKey,
                traceId,
                payload,
                require(publishStatus, "publishStatus"),
                retryCount,
                lastError,
                publishedAt,
                require(createdAt, "createdAt")
        );
    }

    public void markPublished(Instant publishedAt) {
        if (publishStatus == OutboxPublishStatus.PUBLISHED) {
            return;
        }
        this.publishStatus = OutboxPublishStatus.PUBLISHED;
        this.publishedAt = require(publishedAt, "publishedAt");
        this.lastError = null;
    }

    public void markRetryableFailure(String error) {
        if (publishStatus == OutboxPublishStatus.PUBLISHED) {
            return;
        }
        this.publishStatus = OutboxPublishStatus.PENDING;
        this.retryCount++;
        this.lastError = requireText(error, "error");
    }

    public void markFailed(String error) {
        if (publishStatus == OutboxPublishStatus.PUBLISHED) {
            return;
        }
        this.publishStatus = OutboxPublishStatus.FAILED;
        this.retryCount++;
        this.lastError = requireText(error, "error");
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxPublishStatus getPublishStatus() {
        return publishStatus;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다.");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
        }
        return value;
    }
}
