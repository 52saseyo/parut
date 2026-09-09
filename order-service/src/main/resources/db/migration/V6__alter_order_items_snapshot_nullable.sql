-- 타임딜 주문 대응: Product의 타임딜 상품 조회 API는 appearance_type, origin, harvest_date, sale_unit,
-- unit_quantity, original_price를 제공하지 않는다. 일반 주문은 계속 값이 채워지며, 타임딜 주문만 NULL.
ALTER TABLE order_schema.p_order_items
    ALTER COLUMN appearance_type DROP NOT NULL,
    ALTER COLUMN origin DROP NOT NULL,
    ALTER COLUMN harvest_date DROP NOT NULL,
    ALTER COLUMN sale_unit DROP NOT NULL,
    ALTER COLUMN unit_quantity DROP NOT NULL,
    ALTER COLUMN original_price DROP NOT NULL;
