ALTER TABLE product_schema.p_images
    ADD COLUMN uploader_id UUID,
    ADD COLUMN image_url VARCHAR(1000);

UPDATE product_schema.p_images
SET uploader_id = '00000000-0000-0000-0000-000000000000'
WHERE uploader_id IS NULL;

UPDATE product_schema.p_images
SET image_url = 'https://example/' || image_key
WHERE image_url IS NULL;

ALTER TABLE product_schema.p_images
    ALTER COLUMN uploader_id SET NOT NULL,
    ALTER COLUMN image_url SET NOT NULL;

CREATE INDEX idx_p_images_uploader_id
    ON product_schema.p_images (uploader_id);
