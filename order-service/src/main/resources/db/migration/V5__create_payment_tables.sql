-- order_id -> p_orders (Order 애그리거트 논리참조, FK 미사용)
-- user_id -> p_users (user_service 논리참조, FK 미사용)
CREATE TABLE order_schema.p_payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    order_no VARCHAR(30) NOT NULL,
    user_id UUID NOT NULL,
    pg_provider VARCHAR(20) NOT NULL DEFAULT 'TOSS',
    payment_key VARCHAR(200),
    payment_method VARCHAR(30),
    payment_status VARCHAR(30) NOT NULL DEFAULT 'READY',
    total_amount BIGINT NOT NULL,
    balance_amount BIGINT NOT NULL,
    canceled_amount BIGINT NOT NULL DEFAULT 0,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    approved_at TIMESTAMPTZ,
    canceled_at TIMESTAMPTZ,
    receipt_url VARCHAR(500),
    idempotency_key VARCHAR(64) NOT NULL,
    saga_correlation_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(50)
);

-- order_id: 주문당 결제 1건. 재시도 시 새 행을 만들지 않고 같은 행을 재사용
CREATE UNIQUE INDEX uk_payments_order_id
ON order_schema.p_payments (order_id);

-- payment_key: 토스 paymentKey 중복 방지 (승인 전 NULL이므로 NULL은 유니크 제약 대상 아님)
CREATE UNIQUE INDEX uk_payments_payment_key
ON order_schema.p_payments (payment_key);

-- idempotency_key: 결제 승인 API 이중 요청 차단
CREATE UNIQUE INDEX uk_payments_idempotency
ON order_schema.p_payments (idempotency_key);

-- requested_at: 미결 결제 동기화 배치(5분 주기) 대상 조회 전용
CREATE INDEX idx_payments_sync
ON order_schema.p_payments (requested_at)
WHERE payment_status = 'IN_PROGRESS';

CREATE TABLE order_schema.p_payment_transactions (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES order_schema.p_payments (id) ON DELETE RESTRICT,
    transaction_type VARCHAR(30) NOT NULL,
    pg_transaction_key VARCHAR(200),
    amount BIGINT NOT NULL,
    balance_amount_after BIGINT,
    transaction_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    request_payload JSONB,
    response_payload JSONB,
    idempotency_key VARCHAR(64) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(50)
);

-- pg_transaction_key: 토스 transactionKey 중복 방지, PG 대사 조인 키
CREATE UNIQUE INDEX uk_payment_tx_pg_key
ON order_schema.p_payment_transactions (pg_transaction_key);

-- idempotency_key: 이중 취소 차단, 취소 재시도용
CREATE UNIQUE INDEX uk_payment_tx_idempotency
ON order_schema.p_payment_transactions (idempotency_key);
