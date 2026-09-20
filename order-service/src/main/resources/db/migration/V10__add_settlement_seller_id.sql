-- 기존 정산 데이터가 없는 초기 전제이며, 기존 행이 있으면 NOT NULL 컬럼 추가에서 중단된다.
ALTER TABLE order_schema.p_settlements
    ADD COLUMN seller_id UUID NOT NULL;

-- 판매자별 상태 조회와 created_at, id Cursor 정렬에 사용한다.
CREATE INDEX idx_settlements_seller_status_created_id
    ON order_schema.p_settlements (seller_id, status, created_at DESC, id DESC);

-- 관리자 PENDING 대상 조회와 created_at, id Cursor 정렬에 사용한다.
CREATE INDEX idx_settlements_status_created_id
    ON order_schema.p_settlements (status, created_at DESC, id DESC);
