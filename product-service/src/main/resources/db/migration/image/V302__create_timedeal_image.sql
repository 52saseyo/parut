CREATE TABLE product_schema.p_time_deal_images
(
    id           UUID          NOT NULL,
    time_deal_id UUID          NOT NULL,
    image_id     UUID          NOT NULL,
    image_url    VARCHAR(1000) NOT NULL,

    created_at   TIMESTAMPTZ   NOT NULL,
    created_by   VARCHAR(50),
    updated_at   TIMESTAMPTZ,
    updated_by   VARCHAR(50),
    deleted_at   TIMESTAMPTZ,
    deleted_by   VARCHAR(50),

    CONSTRAINT pk_time_deal_images PRIMARY KEY (id)
);