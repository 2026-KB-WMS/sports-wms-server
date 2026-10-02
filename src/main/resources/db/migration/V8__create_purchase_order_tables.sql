-- 창고 발주 테이블: PurchaseOrder, PurchaseOrderLine
-- 참고: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a
-- warehouse_id, supplier_id, sku_id는 이미 있는 테이블이므로 FK를 건다.
-- created_by(User, 회원 도메인)는 참조 테이블이 아직 없어 FK 제약 없이 인덱스만 둔다.
-- User 테이블 생성 마이그레이션에서 ALTER TABLE로 추가한다.

CREATE TABLE purchase_order (
    purchase_order_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_no  VARCHAR(50)   NOT NULL,
    warehouse_id       BIGINT        NOT NULL,
    supplier_id        BIGINT        NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'REQUESTED',
    expected_at        DATETIME      NULL,
    note               VARCHAR(1000) NULL,
    created_by         BIGINT        NOT NULL,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_purchase_order_no UNIQUE (purchase_order_no),
    CONSTRAINT fk_purchase_order_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    CONSTRAINT fk_purchase_order_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id)
);

-- 창고·공급처·상태 필터 목록 조회, 공급처 비활성화 시 진행 중 발주 확인(SUPPLIER_IN_USE)
CREATE INDEX idx_purchase_order_warehouse_status ON purchase_order (warehouse_id, status);
CREATE INDEX idx_purchase_order_supplier_status ON purchase_order (supplier_id, status);
CREATE INDEX idx_purchase_order_created_by ON purchase_order (created_by);

CREATE TABLE purchase_order_line (
    purchase_order_line_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id       BIGINT         NOT NULL,
    sku_id                  BIGINT         NOT NULL,
    expected_quantity       BIGINT         NOT NULL,
    received_quantity       BIGINT         NOT NULL DEFAULT 0,
    line_amount             DECIMAL(18,2)  NOT NULL,
    ordered_unit_price      DECIMAL(18,2)  NOT NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'REQUESTED',
    created_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_purchase_order_line_order_sku UNIQUE (purchase_order_id, sku_id),
    -- 도메인(PurchaseOrderLine.register)을 거치지 않는 경로로 수량이 깨지지 않도록 DB에서도 한 번 더 막는다.
    -- received_quantity의 상한(<= expected_quantity)은 초과 입고 정책이 정해질 때까지 두지 않는다.
    CONSTRAINT ck_purchase_order_line_expected_quantity CHECK (expected_quantity > 0),
    CONSTRAINT ck_purchase_order_line_received_quantity CHECK (received_quantity >= 0),
    CONSTRAINT ck_purchase_order_line_amount CHECK (line_amount >= 0),
    CONSTRAINT ck_purchase_order_line_unit_price CHECK (ordered_unit_price >= 0),
    CONSTRAINT fk_purchase_order_line_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (purchase_order_id),
    CONSTRAINT fk_purchase_order_line_sku FOREIGN KEY (sku_id) REFERENCES product_sku (sku_id)
);

CREATE INDEX idx_purchase_order_line_sku_id ON purchase_order_line (sku_id);
