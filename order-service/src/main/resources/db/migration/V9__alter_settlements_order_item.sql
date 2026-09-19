-- 배송 그룹 단위 정산을 주문상품 단위로 전환한다.
-- 기존 데이터는 주문상품을 임의로 정할 수 없으므로 데이터가 있으면 NOT NULL 제약에서 마이그레이션이 중단된다.
DROP INDEX order_schema.idx_settlements_delivery_group;

ALTER TABLE order_schema.p_settlements
    DROP COLUMN delivery_group_id,
    ADD COLUMN order_item_id UUID NOT NULL;

CREATE UNIQUE INDEX uk_settlements_order_item_id
    ON order_schema.p_settlements (order_item_id);
