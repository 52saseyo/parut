
-- 판매자 테이블
CREATE TABLE user_service.p_sellers (
            id              UUID            NOT NULL,
            login_id        VARCHAR(20)     NOT NULL,
            password        VARCHAR(255)    NOT NULL,
            company_name    VARCHAR(100)    NOT NULL,
            biz_reg_no      VARCHAR(20)     NOT NULL,
            rep_name        VARCHAR(50)     NOT NULL,
            biz_address     VARCHAR(255)    NOT NULL,
            manager_name    VARCHAR(50)     NOT NULL,
            manager_phone   VARCHAR(20)     NOT NULL,
            manager_email   VARCHAR(100)    NOT NULL,
            slack_id        VARCHAR(100)    NOT NULL,
            status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
            approved_by     VARCHAR(30),
            approved_at     TIMESTAMPTZ,
            reject_reason   VARCHAR(255),
            created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_by      VARCHAR(50),
            updated_at      TIMESTAMPTZ,
            updated_by      VARCHAR(50),
            deleted_at      TIMESTAMPTZ,
            deleted_by      VARCHAR(50),

            CONSTRAINT pk_p_sellers PRIMARY KEY (id),
            CONSTRAINT uq_p_sellers_biz_reg_no UNIQUE (biz_reg_no),
            CONSTRAINT chk_p_sellers_status
                CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_p_sellers_login_id ON user_service.p_sellers (login_id) WHERE deleted_at IS NULL;