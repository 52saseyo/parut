CREATE SCHEMA IF NOT EXISTS notification_schema;

CREATE TABLE notification_schema.p_notifications (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    trace_id VARCHAR(100),
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    reference_type VARCHAR(30) NOT NULL,
    reference_id UUID NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uk_notifications_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX idx_notifications_user_created
ON notification_schema.p_notifications (user_id, created_at DESC, id DESC);

CREATE TABLE notification_schema.p_time_deal_notification_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    time_deal_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_time_deal_subscriptions_user_deal UNIQUE (user_id, time_deal_id)
);
