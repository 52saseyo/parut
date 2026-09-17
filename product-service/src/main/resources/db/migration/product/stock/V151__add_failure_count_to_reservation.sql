ALTER TABLE product_schema.p_product_stock_reservations
    ADD COLUMN failure_count INTEGER NOT NULL DEFAULT 0;