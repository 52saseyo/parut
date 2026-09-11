-- user_id -> p_users (user_service 논리참조, FK 미사용)
CREATE TABLE order_schema.p_orders (
    id UUID PRIMARY KEY,
    order_no VARCHAR(30) NOT NULL,
    user_id UUID NOT NULL,
    order_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    order_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    total_product_amount BIGINT NOT NULL DEFAULT 0,
    total_delivery_fee BIGINT NOT NULL DEFAULT 0,
    total_payment_amount BIGINT NOT NULL DEFAULT 0,
    canceled_amount BIGINT NOT NULL DEFAULT 0,
    recipient_name VARCHAR(50) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    address_base VARCHAR(255) NOT NULL,
    address_detail VARCHAR(255),
    delivery_request VARCHAR(255),
    expires_at TIMESTAMPTZ,
    ordered_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    paid_at TIMESTAMPTZ,
    idempotency_key VARCHAR(64) NOT NULL,
    saga_correlation_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(50)
);

-- order_no: 사용자 노출 주문번호 중복 방지, 토스 orderId 매핑
CREATE UNIQUE INDEX uk_orders_order_no
ON order_schema.p_orders (order_no);

-- idempotency_key: 주문 생성 API 중복 요청 차단
CREATE UNIQUE INDEX uk_orders_idempotency
ON order_schema.p_orders (idempotency_key);

-- expires_at: TTL 만료 배치(10초 주기) 대상 조회 전용.
-- 결제 완료·취소된 주문은 배치 대상이 아니므로 PARTIAL 인덱스로 좁혀서 사용.
CREATE INDEX idx_orders_ttl_batch
ON order_schema.p_orders (expires_at)
WHERE order_status IN ('CREATED', 'STOCK_RESERVED', 'PAYMENT_PENDING');

-- seller_id -> p_users (user_service 논리참조, FK 미사용)
CREATE TABLE order_schema.p_order_delivery_groups (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES order_schema.p_orders (id) ON DELETE RESTRICT,
    seller_id UUID NOT NULL,
    group_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    product_amount BIGINT NOT NULL DEFAULT 0,
    delivery_fee BIGINT NOT NULL DEFAULT 0,
    canceled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(50)
);

-- payment_transaction_id -> p_payment_transactions (Payment 애그리거트 논리참조, FK 미사용)
CREATE TABLE order_schema.p_order_cancels (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES order_schema.p_orders (id) ON DELETE RESTRICT,
    cancel_reason_code VARCHAR(30) NOT NULL,
    cancel_reason VARCHAR(500),
    canceled_by_type VARCHAR(20) NOT NULL,
    canceled_by VARCHAR(50) NOT NULL,
    cancel_product_amount BIGINT NOT NULL DEFAULT 0,
    cancel_delivery_fee BIGINT NOT NULL DEFAULT 0,
    cancel_total_amount BIGINT NOT NULL DEFAULT 0,
    refund_required BOOLEAN NOT NULL DEFAULT FALSE,
    payment_transaction_id UUID,
    canceled_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(50)
);

-- product_id, time_deal_id -> product_service 논리참조 (FK 미사용)
-- appearance_type, sale_unit -> Product Service Enum과 동일한 값 사용
CREATE TABLE order_schema.p_order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES order_schema.p_orders (id) ON DELETE RESTRICT,
    delivery_group_id UUID NOT NULL REFERENCES order_schema.p_order_delivery_groups (id) ON DELETE RESTRICT,
    product_id UUID NOT NULL,
    time_deal_id UUID,
    product_name VARCHAR(200) NOT NULL,
    appearance_type VARCHAR(20) NOT NULL,
    origin VARCHAR(50) NOT NULL,
    harvest_date DATE NOT NULL,
    sale_unit VARCHAR(20) NOT NULL,
    unit_quantity NUMERIC(10, 2) NOT NULL,
    original_price BIGINT NOT NULL,
    unit_price BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    item_status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
    cancel_id UUID REFERENCES order_schema.p_order_cancels (id) ON DELETE RESTRICT,
    confirmed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(50)
);

-- append-only 이력 테이블. PK는 삽입 순서 보장을 위해 UUID 대신 BIGINT IDENTITY 사용
-- updated_at/updated_by/version 없음 (수정·삭제하지 않음)
CREATE TABLE order_schema.p_order_status_histories (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES order_schema.p_orders (id) ON DELETE RESTRICT,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    change_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_by VARCHAR(50) NOT NULL
);
