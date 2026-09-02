CREATE TABLE p_time_deal_purchases
(
    id            UUID        NOT NULL PRIMARY KEY,
    order_id      UUID        NOT NULL,
    time_deal_id  UUID        NOT NULL,
    user_id       UUID        NOT NULL,
    quantity      INTEGER     NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    expires_at    TIMESTAMPTZ NOT NULL,
    reserved_at   TIMESTAMPTZ NOT NULL,
    cancel_reason VARCHAR(30),
    created_at    TIMESTAMPTZ NOT NULL,
    created_by    VARCHAR(50) NOT NULL,
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(50),
    deleted_at    TIMESTAMPTZ,
    deleted_by    VARCHAR(50),

    CONSTRAINT uk_time_deal_purchases_order_id UNIQUE (order_id),
    CONSTRAINT ck_time_deal_purchases_status CHECK (status IN ('RESERVED', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_time_deal_purchases_quantity CHECK (quantity >= 1),
    CONSTRAINT ck_time_deal_purchases_expiry CHECK (expires_at > reserved_at)
);

CREATE INDEX idx_time_deal_purchases_time_deal_id ON p_time_deal_purchases (time_deal_id);
CREATE INDEX idx_time_deal_purchases_user_id ON p_time_deal_purchases (user_id);