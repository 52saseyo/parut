-- 취소 API 요청의 중복 처리를 막는 멱등키. PG 호출을 하지 않는 결제 전 취소도 보호 대상이라
-- p_payment_transactions.idempotency_key(PG 호출 중복 방지)와 별도로 둔다.
ALTER TABLE order_schema.p_order_cancels
    ADD COLUMN idempotency_key VARCHAR(64);

UPDATE order_schema.p_order_cancels
SET idempotency_key = id::text
WHERE idempotency_key IS NULL;

ALTER TABLE order_schema.p_order_cancels
    ALTER COLUMN idempotency_key SET NOT NULL;

CREATE UNIQUE INDEX uk_order_cancels_idempotency
    ON order_schema.p_order_cancels (idempotency_key);
