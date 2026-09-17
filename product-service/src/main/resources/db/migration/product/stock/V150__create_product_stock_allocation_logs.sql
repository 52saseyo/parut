CREATE TABLE product_schema.p_product_stock_allocation_logs
(
    id              UUID PRIMARY KEY,
    stock_id        UUID NOT NULL REFERENCES product_schema.p_product_stocks(id),
    event_type      VARCHAR(50) NOT NULL,
    quantity        INTEGER NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    created_by      VARCHAR(50) NOT NULL
);

CREATE INDEX idx_allocation_logs_stock_id ON product_schema.p_product_stock_allocation_logs (stock_id);