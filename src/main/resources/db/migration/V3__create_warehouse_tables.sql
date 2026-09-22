-- 창고 도메인 테이블: Warehouse, WarehouseSection, WarehouseMember
-- 참고: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a
-- WarehouseMember.user_id는 User(회원) 도메인 테이블이 아직 없어 FK 제약 없이 인덱스만 둔다.

CREATE TABLE warehouse (
    warehouse_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_code  VARCHAR(30)    NOT NULL,
    name            VARCHAR(100)   NOT NULL,
    address         VARCHAR(500)   NOT NULL,
    contact_number  VARCHAR(30)    NULL,
    total_capacity  DECIMAL(14,3)  NOT NULL DEFAULT 0,
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_warehouse_code UNIQUE (warehouse_code)
);

CREATE TABLE warehouse_section (
    section_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id       BIGINT         NOT NULL,
    parent_section_id  BIGINT         NULL,
    section_code       VARCHAR(50)    NOT NULL,
    name               VARCHAR(100)   NOT NULL,
    section_type       VARCHAR(20)    NOT NULL,
    capacity           DECIMAL(14,3)  NOT NULL DEFAULT 0,
    current_capacity   DECIMAL(14,3)  NOT NULL DEFAULT 0,
    status             VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_warehouse_section_code UNIQUE (warehouse_id, section_code),
    CONSTRAINT fk_warehouse_section_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    CONSTRAINT fk_warehouse_section_parent FOREIGN KEY (parent_section_id) REFERENCES warehouse_section (section_id)
);

CREATE INDEX idx_warehouse_section_warehouse_id ON warehouse_section (warehouse_id);
CREATE INDEX idx_warehouse_section_parent_section_id ON warehouse_section (parent_section_id);

CREATE TABLE warehouse_member (
    warehouse_member_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id          BIGINT      NOT NULL,
    user_id                BIGINT      NOT NULL,
    member_role            VARCHAR(30) NOT NULL,
    assigned_at             DATETIME    NOT NULL,
    created_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_warehouse_member_warehouse_user UNIQUE (warehouse_id, user_id),
    CONSTRAINT fk_warehouse_member_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id)
);

CREATE INDEX idx_warehouse_member_user_id ON warehouse_member (user_id);
