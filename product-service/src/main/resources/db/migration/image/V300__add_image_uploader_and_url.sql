ALTER TABLE product_schema.p_images
    ADD COLUMN uploader_id UUID,
    ADD COLUMN image_url VARCHAR(1000);

UPDATE product_schema.p_images
SET uploader_id = created_by::UUID
WHERE uploader_id IS NULL
  AND created_by ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$';

UPDATE product_schema.p_images
SET image_url = 'https://example/' || image_key
WHERE image_url IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM product_schema.p_images
        WHERE uploader_id IS NULL
           OR image_url IS NULL
    ) THEN
        RAISE EXCEPTION
            'p_images uploader_id/image_url 백필에 실패한 행이 있습니다.';
END IF;
END
$$;

ALTER TABLE product_schema.p_images
    ALTER COLUMN uploader_id SET NOT NULL,
ALTER COLUMN image_url SET NOT NULL;

CREATE INDEX idx_p_images_uploader_id
    ON product_schema.p_images (uploader_id);