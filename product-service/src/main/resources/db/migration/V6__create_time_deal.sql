CREATE TABLE product_schema.p_time_deals
(
    id                    UUID          NOT NULL PRIMARY KEY,
    product_id            UUID,
    original_price        BIGINT        NOT NULL,
    deal_price            BIGINT        NOT NULL,
    discount_rate         DECIMAL(5, 2) NOT NULL,
    start_at              TIMESTAMPTZ   NOT NULL,
    end_at                TIMESTAMPTZ   NOT NULL,
    max_purchase_quantity INTEGER       NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    created_at            TIMESTAMPTZ   NOT NULL,
    created_by            VARCHAR(50)   NOT NULL,
    updated_at            TIMESTAMPTZ,
    updated_by            VARCHAR(50),
    deleted_at            TIMESTAMPTZ,
    deleted_by            VARCHAR(50),

    CONSTRAINT ck_time_deals_status CHECK (status IN ('SCHEDULED', 'ACTIVE', 'ENDED', 'STOPPED')),
    CONSTRAINT ck_time_deals_period CHECK (end_at > start_at),
    CONSTRAINT ck_time_deals_max_purchase_quantity CHECK (max_purchase_quantity > 0)
);

CREATE INDEX idx_time_deals_product_id ON product_schema.p_time_deals (product_id);
CREATE INDEX idx_time_deals_status ON product_schema.p_time_deals (status);