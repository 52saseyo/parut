-- delivery_group_id -> p_order_delivery_groups (내부 논리참조, FK 미사용)
CREATE TABLE p_deliveries (
    id UUID PRIMARY KEY,
    delivery_group_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
    tracking_number VARCHAR(100),
    shipped_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(50)
);

-- delivery_group_id: 배송 그룹당 배송 1건 (분할배송 제외)
CREATE UNIQUE INDEX idx_deliveries_delivery_group
ON p_deliveries (delivery_group_id);