-- 환불 목록 조회 시 고객과 판매자 소유권을 Refund 테이블에서 직접 확인한다.
-- 기존 데이터는 주문과 배송 그룹에 저장된 소유자로 백필하고 이후 조회에서는 조인하지 않는다.
ALTER TABLE order_schema.p_refunds
    ADD COLUMN customer_id UUID,
    ADD COLUMN seller_id UUID;

UPDATE order_schema.p_refunds r
SET customer_id = o.user_id,
    seller_id = g.seller_id
FROM order_schema.p_order_items i
JOIN order_schema.p_orders o
    ON o.id = i.order_id
JOIN order_schema.p_order_delivery_groups g
    ON g.id = i.delivery_group_id
WHERE r.order_item_id = i.id;

ALTER TABLE order_schema.p_refunds
    ALTER COLUMN customer_id SET NOT NULL,
    ALTER COLUMN seller_id SET NOT NULL;

CREATE INDEX idx_refunds_customer_created_id
    ON order_schema.p_refunds (customer_id, created_at DESC, id DESC);

CREATE INDEX idx_refunds_seller_created_id
    ON order_schema.p_refunds (seller_id, created_at DESC, id DESC);

CREATE INDEX idx_refunds_created_id
    ON order_schema.p_refunds (created_at DESC, id DESC);
