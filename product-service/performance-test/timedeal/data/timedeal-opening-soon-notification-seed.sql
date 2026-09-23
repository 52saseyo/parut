BEGIN;

DELETE FROM product_schema.p_outbox_events
WHERE aggregate_id IN (
                       '81111111-1111-4111-8111-111111111111',
                       '82222222-2222-4222-8222-222222222222',
                       '83333333-3333-4333-8333-333333333333',
                       '84444444-4444-4444-8444-444444444444',
                       '85555555-5555-4555-8555-555555555555'
    );

DELETE FROM product_schema.p_time_deal_stocks
WHERE time_deal_id IN (
                       '81111111-1111-4111-8111-111111111111',
                       '82222222-2222-4222-8222-222222222222',
                       '83333333-3333-4333-8333-333333333333',
                       '84444444-4444-4444-8444-444444444444',
                       '85555555-5555-4555-8555-555555555555'
    );

DELETE FROM product_schema.p_time_deals
WHERE id IN (
             '81111111-1111-4111-8111-111111111111',
             '82222222-2222-4222-8222-222222222222',
             '83333333-3333-4333-8333-333333333333',
             '84444444-4444-4444-8444-444444444444',
             '85555555-5555-4555-8555-555555555555'
    );

INSERT INTO product_schema.p_time_deals (
    id, seller_id, product_id, original_price, deal_price, discount_rate,
    start_at, end_at, max_purchase_quantity, status, name, description,
    product_grade, origin, harvested_date, created_at, created_by
)
VALUES
    (
        '81111111-1111-4111-8111-111111111111',
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
        NULL, 10000, 8000, 20.00,
        CURRENT_TIMESTAMP + INTERVAL '8 minutes',
        CURRENT_TIMESTAMP + INTERVAL '1 hour 8 minutes',
        10, 'SCHEDULED', 'Opening Soon Test Deal 8m',
        'Detector 대상: 현재 시각 기준 8분 후 시작',
        'NORMAL', '국내산', CURRENT_DATE,
        CURRENT_TIMESTAMP, 'performance-test'
    ),
    (
        '82222222-2222-4222-8222-222222222222',
        'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
        NULL, 12000, 9000, 25.00,
        CURRENT_TIMESTAMP + INTERVAL '9 minutes',
        CURRENT_TIMESTAMP + INTERVAL '1 hour 9 minutes',
        10, 'SCHEDULED', 'Opening Soon Test Deal 9m',
        'Detector 대상: 현재 시각 기준 9분 후 시작',
        'NORMAL', '국내산', CURRENT_DATE,
        CURRENT_TIMESTAMP, 'performance-test'
    ),
    (
        '83333333-3333-4333-8333-333333333333',
        'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
        NULL, 15000, 10000, 33.33,
        CURRENT_TIMESTAMP + INTERVAL '10 minutes',
        CURRENT_TIMESTAMP + INTERVAL '1 hour 10 minutes',
        10, 'SCHEDULED', 'Opening Soon Test Deal 10m',
        'Detector 대상: 현재 시각 기준 10분 후 시작',
        'NORMAL', '국내산', CURRENT_DATE,
        CURRENT_TIMESTAMP, 'performance-test'
    ),
    (
        '84444444-4444-4444-8444-444444444444',
        'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
        NULL, 18000, 13000, 27.78,
        CURRENT_TIMESTAMP + INTERVAL '30 minutes',
        CURRENT_TIMESTAMP + INTERVAL '1 hour 30 minutes',
        10, 'SCHEDULED', 'Opening Soon Test Deal 30m',
        'Detector 제외 대상: 시작까지 10분 초과',
        'NORMAL', '국내산', CURRENT_DATE,
        CURRENT_TIMESTAMP, 'performance-test'
    ),
    (
        '85555555-5555-4555-8555-555555555555',
        'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee',
        NULL, 20000, 15000, 25.00,
        CURRENT_TIMESTAMP + INTERVAL '8 minutes',
        CURRENT_TIMESTAMP + INTERVAL '1 hour 8 minutes',
        10, 'ACTIVE', 'Opening Soon Test Deal Active',
        'Detector 제외 대상: SCHEDULED가 아님',
        'NORMAL', '국내산', CURRENT_DATE,
        CURRENT_TIMESTAMP, 'performance-test'
    );

INSERT INTO product_schema.p_time_deal_stocks (
    id, time_deal_id, available_quantity, reserved_quantity, sold_quantity,
    low_stock_threshold, created_at, created_by
)
VALUES
    (
        '81111111-1111-4111-8111-111111111112',
        '81111111-1111-4111-8111-111111111111',
        100, 0, 0, 10, CURRENT_TIMESTAMP, 'performance-test'
    ),
    (
        '82222222-2222-4222-8222-222222222223',
        '82222222-2222-4222-8222-222222222222',
        100, 0, 0, 10, CURRENT_TIMESTAMP, 'performance-test'
    ),
    (
        '83333333-3333-4333-8333-333333333334',
        '83333333-3333-4333-8333-333333333333',
        100, 0, 0, 10, CURRENT_TIMESTAMP, 'performance-test'
    ),
    (
        '84444444-4444-4444-8444-444444444445',
        '84444444-4444-4444-8444-444444444444',
        100, 0, 0, 10, CURRENT_TIMESTAMP, 'performance-test'
    ),
    (
        '85555555-5555-4555-8555-555555555556',
        '85555555-5555-4555-8555-555555555555',
        100, 0, 0, 10, CURRENT_TIMESTAMP, 'performance-test'
    );

COMMIT;