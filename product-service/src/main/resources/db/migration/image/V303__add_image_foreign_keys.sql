ALTER TABLE product_schema.p_product_images
    ADD CONSTRAINT fk_product_images_image
        FOREIGN KEY (image_id)
            REFERENCES product_schema.p_images (id);

ALTER TABLE product_schema.p_time_deal_images
    ADD CONSTRAINT fk_time_deal_images_image
        FOREIGN KEY (image_id)
            REFERENCES product_schema.p_images (id);