-- 회원 도메인 테이블: users (ERD 데이터 사전의 User)
-- 참고: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a
-- 테이블명은 H2·PostgreSQL에서 예약어인 user 대신 users를 쓴다.
-- 다른 테이블의 user_id 계열 컬럼(store_member.user_id, created_by 등)에 대한 FK는
-- 시드 데이터 정리 후 별도 마이그레이션(#168)에서 ALTER TABLE로 추가한다.

CREATE TABLE users (
    user_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id       VARCHAR(50)   NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,
    name           VARCHAR(100)  NOT NULL,
    email          VARCHAR(255)  NOT NULL,
    phone          VARCHAR(30)   NOT NULL,
    role           VARCHAR(30)   NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    last_login_at  DATETIME      NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_login_id UNIQUE (login_id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- GET /users 목록의 role / status 필터용
CREATE INDEX idx_users_role_status ON users (role, status);
