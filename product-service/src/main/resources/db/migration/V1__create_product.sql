CREATE TABLE p_products
(
    id              UUID           NOT NULL,
    seller_id       UUID           NOT NULL,
    category        VARCHAR(30)    NOT NULL,
    name            VARCHAR(150)   NOT NULL,
    description     TEXT,
    price           BIGINT         NOT NULL,
    appearance_type VARCHAR(20)    NOT NULL,
    origin          VARCHAR(100)   NOT NULL,
    harvest_date    DATE           NOT NULL,
    sale_unit       VARCHAR(20)    NOT NULL,
    unit_quantity   DECIMAL(10, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMPTZ    NOT NULL,
    created_by      VARCHAR(50)    NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMPTZ,
    deleted_by      VARCHAR(50),
    CONSTRAINT pk_p_products PRIMARY KEY (id),
    CONSTRAINT ck_p_products_category
        CHECK (category IN ('VEGETABLE', 'FRUIT', 'GRAIN', 'ETC')),
    CONSTRAINT ck_p_products_appearance_type
        CHECK (appearance_type IN ('NORMAL', 'UGLY')),
    CONSTRAINT ck_p_products_sale_unit
        CHECK (sale_unit IN ('G', 'KG', 'EA', 'BOX')),
    CONSTRAINT ck_p_products_status
        CHECK (status IN ('DRAFT', 'ON_SALE', 'SOLD_OUT', 'SUSPENDED', 'DELETED')),
    CONSTRAINT ck_p_products_price CHECK (price >= 0),
    CONSTRAINT ck_p_products_unit_quantity CHECK (unit_quantity > 0)
);

CREATE INDEX idx_p_products_seller_id ON p_products (seller_id);
CREATE INDEX idx_p_products_category_status ON p_products (category, status);
