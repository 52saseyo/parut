-- 역할별 배송 목록을 조인 없이 조회할 수 있도록 생성 시점의 주문과 소유자를 저장한다.
ALTER TABLE order_schema.p_deliveries
    ADD COLUMN order_id UUID,
    ADD COLUMN customer_id UUID,
    ADD COLUMN seller_id UUID;

-- 기존 배송은 배송 그룹과 주문에 저장된 생성 당시 소유자로 채운다.
UPDATE order_schema.p_deliveries d
SET order_id = g.order_id,
    customer_id = o.user_id,
    seller_id = g.seller_id
FROM order_schema.p_order_delivery_groups g
JOIN order_schema.p_orders o
    ON o.id = g.order_id
WHERE d.delivery_group_id = g.id;

ALTER TABLE order_schema.p_deliveries
    ALTER COLUMN order_id SET NOT NULL,
    ALTER COLUMN customer_id SET NOT NULL,
    ALTER COLUMN seller_id SET NOT NULL;

-- 고객과 판매자는 소유자별 커서 조회, 관리자는 전체 최신순 조회에 사용한다.
CREATE INDEX idx_deliveries_customer_created_id
    ON order_schema.p_deliveries (customer_id, created_at DESC, id DESC);

CREATE INDEX idx_deliveries_seller_created_id
    ON order_schema.p_deliveries (seller_id, created_at DESC, id DESC);

CREATE INDEX idx_deliveries_created_id
    ON order_schema.p_deliveries (created_at DESC, id DESC);
