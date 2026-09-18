CREATE TABLE product_schema.p_product_images
(
    id              UUID          NOT NULL,
    product_id      UUID          NOT NULL,
    image_id        UUID          NOT NULL,
    image_url       VARCHAR(1000) NOT NULL,

    created_at      TIMESTAMPTZ   NOT NULL,
    created_by      VARCHAR(50)   NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMPTZ,
    deleted_by      VARCHAR(50),

    CONSTRAINT pk_p_product_images PRIMARY KEY (id)
);