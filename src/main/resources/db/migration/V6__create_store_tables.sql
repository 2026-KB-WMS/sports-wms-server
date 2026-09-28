-- 지점 도메인 테이블: Store, StoreMember
-- 참고: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a
-- StoreMember.user_id는 User(회원) 도메인 테이블이 아직 없어 FK 제약 없이 인덱스만 둔다.
-- (User 테이블 생성 시 ALTER TABLE로 FK를 추가한다.)

CREATE TABLE store (
    store_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_code      VARCHAR(30)   NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    address         VARCHAR(500)  NOT NULL,
    contact_name    VARCHAR(100)  NULL,
    contact_number  VARCHAR(30)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_store_code UNIQUE (store_code)
);

CREATE TABLE store_member (
    store_member_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id         BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL,
    member_role      VARCHAR(30)  NOT NULL,
    assigned_at      DATETIME     NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_store_member_store_user UNIQUE (store_id, user_id),
    CONSTRAINT fk_store_member_store FOREIGN KEY (store_id) REFERENCES store (store_id)
);

CREATE INDEX idx_store_member_user_id ON store_member (user_id);
