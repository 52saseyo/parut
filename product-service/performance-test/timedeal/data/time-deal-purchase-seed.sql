-- 만료된 타임딜 구매 선점 fixture
-- time-deal-seed.sql 실행 후 product-service DB에서 실행한다.
-- 실행할 때마다 30건의 구매가 생성되며, 구매 ID·주문 ID·사용자 ID가 새로 생성된다.

INSERT INTO product_schema.p_time_deal_purchases (
    id,
    order_id,
    time_deal_id,
    user_id,
    quantity,
    status,
    expires_at,
    reserved_at,
    created_at,
    created_by
)
SELECT
    gen_random_uuid(),
    gen_random_uuid(),
    '11111111-1111-4111-8111-111111111111',
    gen_random_uuid(),
    1,
    'RESERVED',
    CURRENT_TIMESTAMP - INTERVAL '10 minutes',
    CURRENT_TIMESTAMP - INTERVAL '20 minutes',
    CURRENT_TIMESTAMP,
    'performance-test'
FROM generate_series(1, 30);
