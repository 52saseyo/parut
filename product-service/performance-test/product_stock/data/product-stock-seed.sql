-- Product Stock performance-test fixtures
-- 실행 대상: product-service DB / PostgreSQL
-- Flyway migration이 완료된 뒤 실행한다.
-- 세 fixture는 성능 테스트 전용 ID를 사용하므로 다른 상품/재고 데이터와 분리한다.

BEGIN;

-- 이전 실행에서 남은 예약·이벤트로그·재고 변동 로그를 먼저 정리한다.
DELETE FROM product_schema.p_product_stock_event_logs
WHERE reservation_id IN (
    SELECT id FROM product_schema.p_product_stock_reservations
    WHERE stock_id IN (
        '33333333-3333-4333-8333-333333333334',
        '44444444-4444-4444-8444-444444444445',
        '55555555-5555-4555-8555-555555555556'
    )
);

DELETE FROM product_schema.p_product_stock_allocation_logs
WHERE stock_id IN (
    '33333333-3333-4333-8333-333333333334',
    '44444444-4444-4444-8444-444444444445',
    '55555555-5555-4555-8555-555555555556'
);

DELETE FROM product_schema.p_product_stock_reservations
WHERE stock_id IN (
    '33333333-3333-4333-8333-333333333334',
    '44444444-4444-4444-8444-444444444445',
    '55555555-5555-4555-8555-555555555556'
);

DELETE FROM product_schema.p_product_stocks
WHERE id IN (
    '33333333-3333-4333-8333-333333333334',
    '44444444-4444-4444-8444-444444444445',
    '55555555-5555-4555-8555-555555555556'
);

DELETE FROM product_schema.p_products
WHERE id IN (
    '33333333-3333-4333-8333-333333333333',
    '44444444-4444-4444-8444-444444444444',
    '55555555-5555-4555-8555-555555555555'
);

-- Concurrency Test fixture (reserve)
-- 초기 재고 100개, 500건의 동시 reserve 요청 중 재고 정합성만 검증한다.
INSERT INTO product_schema.p_products (
    id, seller_id, category, name, description, price, appearance_type,
    origin, harvest_date, sale_unit, unit_quantity, status,
    created_at, created_by
)
VALUES (
    '33333333-3333-4333-8333-333333333333',
    'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
    'VEGETABLE',
    'Concurrency Test Product',
    'JMeter 500개 동시 reserve 선점 검증용 fixture',
    10000, 'NORMAL',
    '국내산', CURRENT_DATE, 'EA', 1,
    'ON_SALE',
    CURRENT_TIMESTAMP, 'performance-test'
);

INSERT INTO product_schema.p_product_stocks (
    id, product_id, total_quantity, available_quantity,
    low_stock_threshold, status, version, created_at, created_by
)
VALUES (
    '33333333-3333-4333-8333-333333333334',
    '33333333-3333-4333-8333-333333333333',
    100, 100, 10, 'AVAILABLE', 0, CURRENT_TIMESTAMP, 'performance-test'
);

-- Update Stock 교차 충돌 Test fixture
-- 예약이 없는(reservedQuantity = 0) 재고라 어떤 totalQuantity 값으로 갱신해도 유효성 검증에는 걸리지 않고,
-- 낙관적 락(version) 충돌만 순수하게 재현할 수 있다.
INSERT INTO product_schema.p_products (
    id, seller_id, category, name, description, price, appearance_type,
    origin, harvest_date, sale_unit, unit_quantity, status,
    created_at, created_by
)
VALUES (
    '44444444-4444-4444-8444-444444444444',
    'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
    'VEGETABLE',
    'UpdateStock Conflict Test Product',
    'JMeter 10 Thread updateStock 교차 충돌 검증용 fixture',
    10000, 'NORMAL',
    '국내산', CURRENT_DATE, 'EA', 1,
    'ON_SALE',
    CURRENT_TIMESTAMP, 'performance-test'
);

INSERT INTO product_schema.p_product_stocks (
    id, product_id, total_quantity, available_quantity,
    low_stock_threshold, status, version, created_at, created_by
)
VALUES (
    '44444444-4444-4444-8444-444444444445',
    '44444444-4444-4444-8444-444444444444',
    1000, 1000, 10, 'AVAILABLE', 0, CURRENT_TIMESTAMP, 'performance-test'
);

-- 격리된 예약(EXPIRATION_FAILED) Test fixture
-- 만료 처리 배치가 재고 복구(RESTORE)에 실패해 격리된 상태를 직접 재현한다.
-- 재고 50개 중 20개가 예약에 물려 있는 채로(available_quantity=30) 격리되어 있어,
-- GET /api/v1/stocks/reservations/isolated 와 POST /api/v1/stocks/reservations/{reservationId}/recover 를 바로 검증할 수 있다.
INSERT INTO product_schema.p_products (
    id, seller_id, category, name, description, price, appearance_type,
    origin, harvest_date, sale_unit, unit_quantity, status,
    created_at, created_by
)
VALUES (
    '55555555-5555-4555-8555-555555555555',
    'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee',
    'VEGETABLE',
    'Isolated Reservation Test Product',
    '격리된 예약(EXPIRATION_FAILED) 조회·복구 검증용 fixture',
    10000, 'NORMAL',
    '국내산', CURRENT_DATE, 'EA', 1,
    'ON_SALE',
    CURRENT_TIMESTAMP, 'performance-test'
);

INSERT INTO product_schema.p_product_stocks (
    id, product_id, total_quantity, available_quantity,
    low_stock_threshold, status, version, created_at, created_by
)
VALUES (
    '55555555-5555-4555-8555-555555555556',
    '55555555-5555-4555-8555-555555555555',
    50, 30, 10, 'AVAILABLE', 0, CURRENT_TIMESTAMP, 'performance-test'
);

INSERT INTO product_schema.p_product_stock_reservations (
    id, stock_id, order_id, quantity, status, expires_at,
    version, created_at, created_by
)
VALUES (
    '55555555-5555-4555-8555-555555555557',
    '55555555-5555-4555-8555-555555555556',
    '55555555-5555-4555-8555-555555555558',
    20, 'EXPIRATION_FAILED',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    0, CURRENT_TIMESTAMP, 'performance-test'
);

INSERT INTO product_schema.p_product_stock_event_logs (
    id, reservation_id, order_item_id, event_type, processed_at,
    created_at, created_by
)
VALUES (
    '55555555-5555-4555-8555-555555555559',
    '55555555-5555-4555-8555-555555555557',
    '55555555-5555-4555-8555-55555555555a',
    'RESERVE', CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP - INTERVAL '1 day', 'performance-test'
);

COMMIT;
