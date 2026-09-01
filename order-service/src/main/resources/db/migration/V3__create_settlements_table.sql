-- delivery_group_id -> p_order_delivery_groups (내부 논리참조, FK 미사용)
-- processed_by -> p_users (user_service 논리참조, FK 미사용)
CREATE TABLE p_settlements (
    id UUID PRIMARY KEY,
    delivery_group_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sales_amount BIGINT NOT NULL CHECK (sales_amount >= 0),
    -- settlement_amount: V0 범위에서는 sales_amount와 동일
    -- (수수료, 정산보류금 미적용). 추후 수수료 정책 도입 시
    -- sales_amount와 분리될 수 있음
    settlement_amount BIGINT NOT NULL,
    eligible_at TIMESTAMPTZ NOT NULL,
    settled_at TIMESTAMPTZ,
    processed_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(50)
);

-- delivery_group_id: 배송 그룹당 정산 1건
CREATE UNIQUE INDEX idx_settlements_delivery_group
ON p_settlements (delivery_group_id);