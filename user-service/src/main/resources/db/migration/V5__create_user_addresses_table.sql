-- 사용자 배송지 테이블
CREATE TABLE user_schema.p_user_addresses (
            id                UUID            NOT NULL,
            user_id           UUID            NOT NULL,
            address_name      VARCHAR(30)     NOT NULL,
            recipient_name    VARCHAR(50)     NOT NULL,
            recipient_phone   VARCHAR(20)     NOT NULL,
            zip_code          VARCHAR(10)     NOT NULL,
            address_base      VARCHAR(255)    NOT NULL,
            address_detail    VARCHAR(255),
            is_default        BOOLEAN         NOT NULL DEFAULT FALSE,
            created_at        TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_by        VARCHAR(50)     NOT NULL,
            updated_at        TIMESTAMPTZ,
            updated_by        VARCHAR(50),
            deleted_at        TIMESTAMPTZ,
            deleted_by        VARCHAR(50),

            CONSTRAINT pk_p_user_addresses PRIMARY KEY (id),
            CONSTRAINT fk_p_user_addresses_user
                FOREIGN KEY (user_id) REFERENCES user_schema.p_users (id)
);

-- 사용자별 활성 배송지 목록 조회용 인덱스
CREATE INDEX idx_p_user_addresses_user_active
    ON user_schema.p_user_addresses (user_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- 사용자별 활성 기본 배송지는 최대 한 개
CREATE UNIQUE INDEX uq_p_user_addresses_default_active
    ON user_schema.p_user_addresses (user_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;
