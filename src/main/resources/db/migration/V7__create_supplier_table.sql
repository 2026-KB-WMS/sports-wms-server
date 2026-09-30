-- 공급처(Supplier) 테이블
-- 참고: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a
-- Lot.supplier_id는 V4에서 FK 제약 없이 인덱스만 두었다. 이제 Supplier 테이블이 생겼으니 FK를 추가한다.

CREATE TABLE supplier (
    supplier_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_code   VARCHAR(30)   NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    manager_name    VARCHAR(100)  NOT NULL,
    contact_number  VARCHAR(30)   NOT NULL,
    email           VARCHAR(255)  NULL,
    address         VARCHAR(500)  NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_supplier_code UNIQUE (supplier_code)
);

ALTER TABLE lot
    ADD CONSTRAINT fk_lot_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id);
