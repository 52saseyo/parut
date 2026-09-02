CREATE TABLE product_service.p_product_stock_reservations(
    id UUID primary key,
    stock_id UUID NOT NULL REFERENCES product_service.p_product_stocks(id),
    order_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(50),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(50)
)

CREATE INDEX idx_reservation_stock_id ON product_service.p_product_stock_reservations(stock_id);
CREATE INDEX idx_reservation_order_id ON product_service.p_product_stock_reservations(order_id);
CREATE INDEX idx_reservation_status_expires ON product_service.p_product_stock_reservations(status, expires_at);