-- 타임딜 상품 조회 내부 API 확정 스펙: productId는 전환 출처 상품 ID라 직접 등록 타임딜은 NULL로 내려온다.
-- 일반 주문은 계속 값이 채워지며, 직접 등록 타임딜 주문만 NULL.
ALTER TABLE order_schema.p_order_items
    ALTER COLUMN product_id DROP NOT NULL;
