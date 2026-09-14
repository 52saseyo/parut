CREATE TABLE product_schema.p_time_deals
(
    id                    UUID          NOT NULL PRIMARY KEY,
    seller_id             UUID          NOT NULL,
    product_id            UUID,
    image_id              UUID,
    original_price        BIGINT        NOT NULL,
    deal_price            BIGINT        NOT NULL,
    discount_rate         DECIMAL(5, 2) NOT NULL,
    start_at              TIMESTAMPTZ   NOT NULL,
    end_at                TIMESTAMPTZ   NOT NULL,
    max_purchase_quantity INTEGER       NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    name                  VARCHAR(150)  NOT NULL,
    description           TEXT,
    product_grade         VARCHAR(20)   NOT NULL,
    origin                VARCHAR(100)  NOT NULL,
    harvested_date        DATE          NOT NULL,
    created_at            TIMESTAMPTZ   NOT NULL,
    created_by            VARCHAR(50)   NOT NULL,
    updated_at            TIMESTAMPTZ,
    updated_by            VARCHAR(50),
    deleted_at            TIMESTAMPTZ,
    deleted_by            VARCHAR(50),

    CONSTRAINT ck_time_deals_status CHECK (status IN ('SCHEDULED', 'ACTIVE', 'ENDED', 'STOPPED')),
    CONSTRAINT ck_time_deals_product_grade CHECK (product_grade IN ('NORMAL', 'UGLY')),
    CONSTRAINT ck_time_deals_period CHECK (end_at > start_at),
    CONSTRAINT ck_time_deals_max_purchase_quantity CHECK (max_purchase_quantity > 0)
);

CREATE INDEX idx_time_deals_seller_id ON product_schema.p_time_deals (seller_id);
CREATE INDEX idx_time_deals_product_id ON product_schema.p_time_deals (product_id);
CREATE INDEX idx_time_deals_status_start_at ON product_schema.p_time_deals (status, start_at);
CREATE INDEX idx_time_deals_status_end_at ON product_schema.p_time_deals (status, end_at);

-- 전환 중복 방지: 같은 상품을 같은 판매 기간으로 두 번 전환할 수 없다.
-- 재시도로 일반 상품 재고가 두 번 차감되는 것을 막는 최종 방어선이며,
-- 직접 등록(product_id IS NULL)과 삭제된 타임딜은 대상에서 뺀다.
CREATE UNIQUE INDEX uq_time_deals_active_conversion
    ON product_schema.p_time_deals (product_id, start_at, end_at)
    WHERE product_id IS NOT NULL
      AND deleted_at IS NULL;
