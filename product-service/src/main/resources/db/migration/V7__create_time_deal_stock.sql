CREATE TABLE product_schema.p_time_deal_stocks
(
    id                  UUID        NOT NULL PRIMARY KEY,
    time_deal_id        UUID        NOT NULL,
    available_quantity  INTEGER     NOT NULL DEFAULT 0,
    reserved_quantity   INTEGER     NOT NULL DEFAULT 0,
    sold_quantity       INTEGER     NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER     NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    created_by          VARCHAR(50) NOT NULL,
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(50),
    deleted_at          TIMESTAMPTZ,
    deleted_by          VARCHAR(50),

    CONSTRAINT uk_time_deal_stocks_time_deal_id UNIQUE (time_deal_id),
    CONSTRAINT ck_time_deal_stocks_available_quantity CHECK (available_quantity >= 0),
    CONSTRAINT ck_time_deal_stocks_reserved_quantity CHECK (reserved_quantity >= 0),
    CONSTRAINT ck_time_deal_stocks_sold_quantity CHECK (sold_quantity >= 0),
    CONSTRAINT ck_time_deal_stocks_low_stock_threshold CHECK (low_stock_threshold >= 0)
);