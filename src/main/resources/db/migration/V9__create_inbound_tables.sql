-- 입고 도메인 테이블: Inbound, InboundLine
-- 기준: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a
-- purchase_order, warehouse, purchase_order_line, lot, warehouse_section은 이미 있는 테이블이므로 FK를 건다.
-- received_by(User, 회원 도메인)는 아직 테이블이 없어 FK 없이 인덱스만 둔다.
-- User 테이블 생성 마이그레이션에서 ALTER TABLE로 FK를 추가한다.

CREATE TABLE inbound (
    inbound_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    inbound_no         VARCHAR(50)   NOT NULL,
    purchase_order_id  BIGINT        NOT NULL,
    warehouse_id       BIGINT        NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'ARRIVED',
    arrived_at         DATETIME      NOT NULL,
    received_at        DATETIME      NULL,
    received_by        BIGINT        NULL,
    note               VARCHAR(1000) NULL,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_inbound_no UNIQUE (inbound_no),
    CONSTRAINT fk_inbound_purchase_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (purchase_order_id),
    CONSTRAINT fk_inbound_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id)
);

-- 발주별 입고 목록·진행 중 입고 확인(발주 취소 가드), 창고별 상태 목록 조회
CREATE INDEX idx_inbound_purchase_order_status ON inbound (purchase_order_id, status);
CREATE INDEX idx_inbound_warehouse_status ON inbound (warehouse_id, status);
CREATE INDEX idx_inbound_received_by ON inbound (received_by);

CREATE TABLE inbound_line (
    inbound_line_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    inbound_id             BIGINT         NOT NULL,
    purchase_order_line_id BIGINT         NOT NULL,
    lot_id                 BIGINT         NOT NULL,
    accepted_section_id    BIGINT         NULL,
    defect_section_id      BIGINT         NULL,
    received_quantity      BIGINT         NOT NULL,
    accepted_quantity      BIGINT         NOT NULL DEFAULT 0,
    defective_quantity     BIGINT         NOT NULL DEFAULT 0,
    received_unit_price    DECIMAL(18,2)  NOT NULL,
    line_amount            DECIMAL(18,2)  NOT NULL,
    price_change_reason    VARCHAR(500)   NULL,
    inspection_note        VARCHAR(1000)  NULL,
    received_at            DATETIME       NOT NULL,
    received_by            BIGINT         NOT NULL,
    created_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 한 발주 항목을 여러 로트로 나눠 받을 수 있으므로 (입고, 발주 항목, 로트)가 한 줄이다.
    CONSTRAINT uk_inbound_line_inbound_po_line_lot UNIQUE (inbound_id, purchase_order_line_id, lot_id),
    -- 도메인(InboundLine.register)을 거치지 않는 경로로 잘못된 수량이 들어오지 않도록 DB에서도 한 번 더 막는다.
    CONSTRAINT ck_inbound_line_received_quantity CHECK (received_quantity > 0),
    CONSTRAINT ck_inbound_line_accepted_quantity CHECK (accepted_quantity >= 0),
    CONSTRAINT ck_inbound_line_defective_quantity CHECK (defective_quantity >= 0),
    CONSTRAINT ck_inbound_line_quantity_sum CHECK (accepted_quantity + defective_quantity = received_quantity),
    CONSTRAINT ck_inbound_line_unit_price CHECK (received_unit_price >= 0),
    CONSTRAINT ck_inbound_line_amount CHECK (line_amount >= 0),
    CONSTRAINT fk_inbound_line_inbound FOREIGN KEY (inbound_id) REFERENCES inbound (inbound_id),
    CONSTRAINT fk_inbound_line_purchase_order_line FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line (purchase_order_line_id),
    CONSTRAINT fk_inbound_line_lot FOREIGN KEY (lot_id) REFERENCES lot (lot_id),
    CONSTRAINT fk_inbound_line_accepted_section FOREIGN KEY (accepted_section_id) REFERENCES warehouse_section (section_id),
    CONSTRAINT fk_inbound_line_defect_section FOREIGN KEY (defect_section_id) REFERENCES warehouse_section (section_id)
);

-- 발주 항목별 입고 누계, 로트·구역별 입고 이력 조회
CREATE INDEX idx_inbound_line_purchase_order_line_id ON inbound_line (purchase_order_line_id);
CREATE INDEX idx_inbound_line_lot_id ON inbound_line (lot_id);
CREATE INDEX idx_inbound_line_accepted_section_id ON inbound_line (accepted_section_id);
CREATE INDEX idx_inbound_line_defect_section_id ON inbound_line (defect_section_id);
CREATE INDEX idx_inbound_line_received_by ON inbound_line (received_by);
