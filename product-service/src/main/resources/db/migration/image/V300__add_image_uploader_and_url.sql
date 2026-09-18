ALTER TABLE product_schema.p_images
    ADD COLUMN uploader_id UUID,
    ADD COLUMN image_url VARCHAR(1000);

ALTER TABLE product_schema.p_images
    ALTER COLUMN uploader_id SET NOT NULL,
    ALTER COLUMN image_url SET NOT NULL;

CREATE INDEX idx_p_images_uploader_id
    ON product_schema.p_images(uploader_id);