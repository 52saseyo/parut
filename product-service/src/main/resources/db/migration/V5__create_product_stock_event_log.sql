CREATE TABLE product_service.p_product_stock_event_logs
(
    id UUID primary key,
    reservation_id UUID NOT NULL REFERENCES product_service.p_product_stock_reservations(id),
    order_item_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX uq_event_logs_orderItem_type ON product_service.p_product_stock_event_logs(order_item_id, event_type);