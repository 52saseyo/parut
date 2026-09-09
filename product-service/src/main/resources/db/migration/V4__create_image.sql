CREATE TABLE product_schema.p_images
(
    id            UUID         NOT NULL,
    image_key     VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    file_size     BIGINT       NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    created_by    VARCHAR(50)  NOT NULL,
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(50),
    deleted_at    TIMESTAMPTZ,
    deleted_by    VARCHAR(50),
    CONSTRAINT pk_p_images PRIMARY KEY (id),
    CONSTRAINT uq_p_images_image_key UNIQUE (image_key),
    CONSTRAINT ck_p_images_content_type
        CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT ck_p_images_file_size
        CHECK (file_size > 0 AND file_size <= 10485760)
);

ALTER TABLE product_schema.p_products
    ADD COLUMN image_id UUID;

ALTER TABLE product_schema.p_products
    ADD CONSTRAINT fk_p_products_image
        FOREIGN KEY (image_id) REFERENCES product_schema.p_images (id);

CREATE INDEX idx_p_products_image_id
    ON product_schema.p_products (image_id)
    WHERE image_id IS NOT NULL;
