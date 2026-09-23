-- 재고 도메인 테이블: Lot, InventoryLot, InventoryTransaction
-- 참고: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a
-- lot.supplier_id(Supplier, 입고 도메인)와 inventory_transaction.created_by(User, 회원 도메인)는
-- 참조 테이블이 아직 없어 FK 제약 없이 인덱스만 둔다. 해당 테이블 생성 마이그레이션에서 ALTER TABLE로 추가한다.

CREATE TABLE lot (
    lot_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id             BIGINT         NOT NULL,
    supplier_id        BIGINT         NOT NULL,
    lot_number         VARCHAR(100)   NOT NULL,
    manufactured_date  DATE           NULL,
    expiry_date        DATE           NULL,
    status             VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    unit_cost          DECIMAL(18,2)  NOT NULL,
    created_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_lot_sku_supplier_number UNIQUE (sku_id, supplier_id, lot_number),
    CONSTRAINT ck_lot_unit_cost CHECK (unit_cost >= 0),
    CONSTRAINT fk_lot_sku FOREIGN KEY (sku_id) REFERENCES product_sku (sku_id)
);

CREATE INDEX idx_lot_supplier_id ON lot (supplier_id);

CREATE TABLE inventory_lot (
    inventory_lot_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id          BIGINT       NOT NULL,
    lot_id              BIGINT       NOT NULL,
    on_hand_quantity    BIGINT       NOT NULL DEFAULT 0,
    allocated_quantity  BIGINT       NOT NULL DEFAULT 0,
    quality_status      VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    last_counted_at     DATETIME     NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_lot_section_lot UNIQUE (section_id, lot_id),
    -- 동시성 버그 등으로 수량이 깨지지 않도록 DB에서도 한 번 더 막는다: 0 <= allocated <= on_hand
    CONSTRAINT ck_inventory_lot_quantity CHECK (allocated_quantity >= 0 AND allocated_quantity <= on_hand_quantity),
    CONSTRAINT fk_inventory_lot_section FOREIGN KEY (section_id) REFERENCES warehouse_section (section_id),
    CONSTRAINT fk_inventory_lot_lot FOREIGN KEY (lot_id) REFERENCES lot (lot_id)
);

CREATE INDEX idx_inventory_lot_lot_id ON inventory_lot (lot_id);

CREATE TABLE inventory_transaction (
    transaction_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_lot_id  BIGINT        NOT NULL,
    transaction_type  VARCHAR(30)   NOT NULL,
    quantity_delta    BIGINT        NOT NULL,
    before_quantity   BIGINT        NOT NULL,
    after_quantity    BIGINT        NOT NULL,
    reference_type    VARCHAR(30)   NOT NULL,
    reference_id      BIGINT        NULL,
    reason            VARCHAR(500)  NULL,
    created_by        BIGINT        NOT NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_transaction_inventory_lot FOREIGN KEY (inventory_lot_id) REFERENCES inventory_lot (inventory_lot_id)
);

-- 재고별 이력 조회(GET /inventory/{id}/transactions)와 전체 이력 기간 조회(GET /inventory/transactions)
CREATE INDEX idx_inventory_transaction_lot_created ON inventory_transaction (inventory_lot_id, created_at);
CREATE INDEX idx_inventory_transaction_created_at ON inventory_transaction (created_at);
CREATE INDEX idx_inventory_transaction_reference ON inventory_transaction (reference_type, reference_id);
CREATE INDEX idx_inventory_transaction_created_by ON inventory_transaction (created_by);
