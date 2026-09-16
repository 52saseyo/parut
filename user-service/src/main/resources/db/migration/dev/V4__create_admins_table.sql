-- 1. pgcrypto 확장 기능 활성화 (없으면 생성)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 2. 관리자 테이블 생성
CREATE TABLE user_schema.p_admins (
            id              UUID            NOT NULL,
            username        VARCHAR(20)     NOT NULL,
            password        VARCHAR(255)    NOT NULL,
            role            VARCHAR(10)     NOT NULL,
            created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_by      VARCHAR(50),
            updated_at      TIMESTAMPTZ,
            updated_by      VARCHAR(50),
            deleted_at      TIMESTAMPTZ,
            deleted_by      VARCHAR(50),

            CONSTRAINT pk_p_admins PRIMARY KEY (id),
            CONSTRAINT uq_p_admins_username UNIQUE (username)
);

-- 3. 인덱스 생성
CREATE INDEX idx_p_admins_username ON user_schema.p_admins (username) WHERE deleted_at IS NULL;

-- 4. 초기 데이터 삽입 (암호화 적용)
INSERT INTO user_schema.p_admins (id, username, password, role, created_by)
VALUES
    ('00000000-0000-0000-0000-000000000000', 'system', 'NOLOGIN_SYSTEM', 'SYSTEM', 'system'),
    ('00000000-0000-0000-0000-000000000001', 'batch', 'NOLOGIN_BATCH', 'BATCH', 'system'),
    ('99999999-9999-9999-9999-999999999999', 'admin', crypt('admin123', gen_salt('bf')), 'ADMIN', 'system');