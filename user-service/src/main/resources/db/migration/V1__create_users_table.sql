
-- 사용자 테이블
CREATE TABLE user_service.p_users (
          id          UUID            NOT NULL,
          username    VARCHAR(20)     NOT NULL,
          password    VARCHAR(100)    NOT NULL,
          name        VARCHAR(100)    NOT NULL,
          slack_id    VARCHAR(100)    NOT NULL,
          created_at  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
          created_by  VARCHAR(50)     NOT NULL,
          updated_at  TIMESTAMPTZ,
          updated_by  VARCHAR(50),
          deleted_at  TIMESTAMPTZ,
          deleted_by  VARCHAR(50),

          CONSTRAINT pk_p_users PRIMARY KEY (id),
          CONSTRAINT uq_p_users_username UNIQUE (username)
);


CREATE UNIQUE INDEX uq_p_users_username_active ON user_service.p_users (username) WHERE deleted_at IS NULL;