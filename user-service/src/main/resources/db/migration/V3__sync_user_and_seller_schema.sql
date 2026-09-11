-- 1. p_users 테이블 정합성 보정
ALTER TABLE user_schema.p_users
    ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER';

ALTER TABLE user_schema.p_users
    ALTER COLUMN slack_id DROP NOT NULL;

-- 2. p_sellers 테이블 정합성 보정
ALTER TABLE user_schema.p_sellers
    ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'SELLER';

ALTER TABLE user_schema.p_sellers
    ALTER COLUMN slack_id DROP NOT NULL;

ALTER TABLE user_schema.p_sellers
    ALTER COLUMN created_by SET NOT NULL;