-- =====================================================
-- V13: 출고 도메인 테이블 (stock_allocation, outbound, outbound_line)
-- =====================================================
-- allocated_by, shipped_by 는 사용자 FK 컬럼이나 인덱스만 두고 FK 는 걸지 않는다.

-- 재고 할당
CREATE TABLE stock_allocation
(
    allocation_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_order_line_id BIGINT      NOT NULL,
    inventory_lot_id    BIGINT      NOT NULL,
    allocated_quantity  BIGINT      NOT NULL,
    picked_quantity     BIGINT      NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'ALLOCATED',
    allocated_at        DATETIME    NOT NULL,
    released_at         DATETIME    NULL,
    allocated_by        BIGINT      NULL,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_allocation_store_order_line FOREIGN KEY (store_order_line_id) REFERENCES store_order_line (store_order_line_id),
    CONSTRAINT fk_stock_allocation_inventory_lot FOREIGN KEY (inventory_lot_id) REFERENCES inventory_lot (inventory_lot_id),
    CONSTRAINT ck_stock_allocation_allocated_quantity CHECK (allocated_quantity > 0),
    CONSTRAINT ck_stock_allocation_picked_quantity CHECK (picked_quantity >= 0 AND picked_quantity <= allocated_quantity),
    CONSTRAINT ck_stock_allocation_status CHECK (status IN ('ALLOCATED', 'PICKED', 'RELEASED'))
);

CREATE INDEX idx_stock_allocation_line_status ON stock_allocation (store_order_line_id, status);
CREATE INDEX idx_stock_allocation_lot_status ON stock_allocation (inventory_lot_id, status);
CREATE INDEX idx_stock_allocation_allocated_by ON stock_allocation (allocated_by);

-- 출고
CREATE TABLE outbound
(
    outbound_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbound_no    VARCHAR(30)  NOT NULL,
    store_order_id BIGINT       NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'READY',
    shipped_at     DATETIME     NULL,
    shipped_by     BIGINT       NULL,
    delivered_at   DATETIME     NULL,
    note           VARCHAR(500) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_outbound_no UNIQUE (outbound_no),
    CONSTRAINT fk_outbound_store_order FOREIGN KEY (store_order_id) REFERENCES store_order (store_order_id),
    CONSTRAINT ck_outbound_status CHECK (status IN ('READY', 'PICKING', 'PICKED', 'SHIPPED', 'DELIVERED', 'CANCELED'))
);

CREATE INDEX idx_outbound_order_status ON outbound (store_order_id, status);
CREATE INDEX idx_outbound_status_created ON outbound (status, created_at);
CREATE INDEX idx_outbound_shipped_by ON outbound (shipped_by);

-- 출고 항목
CREATE TABLE outbound_line
(
    outbound_line_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbound_id                BIGINT         NOT NULL,
    allocation_id              BIGINT         NOT NULL,
    shipped_quantity           BIGINT         NOT NULL DEFAULT 0,
    confirmed_unit_supply_price DECIMAL(18, 2) NULL,
    created_at                 DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_outbound_line_outbound_allocation UNIQUE (outbound_id, allocation_id),
    CONSTRAINT fk_outbound_line_outbound FOREIGN KEY (outbound_id) REFERENCES outbound (outbound_id),
    CONSTRAINT fk_outbound_line_allocation FOREIGN KEY (allocation_id) REFERENCES stock_allocation (allocation_id),
    CONSTRAINT ck_outbound_line_shipped_quantity CHECK (shipped_quantity >= 0),
    CONSTRAINT ck_outbound_line_unit_supply_price CHECK (confirmed_unit_supply_price IS NULL OR confirmed_unit_supply_price >= 0)
);

CREATE INDEX idx_outbound_line_allocation ON outbound_line (allocation_id);
