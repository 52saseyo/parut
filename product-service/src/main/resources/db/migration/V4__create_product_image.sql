CREATE TABLE product_schema.p_product_images
(
    id          UUID         NOT NULL,
    product_id  UUID         NOT NULL,
    image_key   VARCHAR(500) NOT NULL,
    image_type  VARCHAR(20)  NOT NULL,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL,
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(50),
    deleted_at  TIMESTAMPTZ,
    deleted_by  VARCHAR(50),
    CONSTRAINT pk_p_product_images PRIMARY KEY (id),
    CONSTRAINT fk_p_product_images_product
        FOREIGN KEY (product_id) REFERENCES product_schema.p_products (id),
    CONSTRAINT ck_p_product_images_type CHECK (image_type IN ('MAIN', 'DETAIL')),
    CONSTRAINT ck_p_product_images_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX idx_p_product_images_product_sort
    ON product_schema.p_product_images (product_id, sort_order);
