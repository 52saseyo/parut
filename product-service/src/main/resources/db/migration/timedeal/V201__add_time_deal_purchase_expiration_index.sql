CREATE INDEX idx_time_deal_purchases_status_expires_at_id
    ON product_schema.p_time_deal_purchases (status, expires_at, id);
