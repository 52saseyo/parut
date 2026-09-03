-- order_item_id -> p_order_items (내부 논리참조, FK 미사용)
CREATE TABLE order_schema.p_refunds (
    id UUID PRIMARY KEY,
    order_item_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    refund_amount BIGINT NOT NULL CHECK (refund_amount >= 0), -- 수량 일부 환불 미지원, 전체 금액만
    reason VARCHAR(500) NOT NULL,
    rejection_reason VARCHAR(500),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    canceled_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    processed_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(50)
);

-- CANCELED 상태 요청은 유일성 제약 대상에서 제외해 재요청 시 새 행을 허용.
-- 활성(REQUESTED) 또는 최종 처리된(APPROVED/REJECTED) 요청만
-- 주문상품당 1건으로 제한.
CREATE UNIQUE INDEX idx_refunds_active_order_item
ON order_schema.p_refunds (order_item_id)
WHERE status != 'CANCELED';
