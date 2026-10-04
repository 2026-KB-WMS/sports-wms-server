-- 지점 발주 테이블: StoreOrder, StoreOrderLine
-- 기준: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a
-- store, warehouse, product_sku는 이미 있는 테이블이므로 FK를 건다.
-- warehouse_id는 창고 배정(ASSIGNED) 전까지 NULL이다.
-- created_by(User, 회원 도메인)는 아직 테이블이 없어 FK 없이 인덱스만 둔다.
-- User 테이블 생성 마이그레이션에서 ALTER TABLE로 FK를 추가한다.

CREATE TABLE store_order (
    store_order_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no              VARCHAR(50)   NOT NULL,
    store_id              BIGINT        NOT NULL,
    warehouse_id          BIGINT        NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'REQUESTED',
    requested_at          DATETIME      NOT NULL,
    requested_delivery_at DATETIME      NULL,
    note                  VARCHAR(1000) NULL,
    created_by            BIGINT        NOT NULL,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_store_order_no UNIQUE (order_no),
    CONSTRAINT fk_store_order_store FOREIGN KEY (store_id) REFERENCES store (store_id),
    CONSTRAINT fk_store_order_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id)
);

-- 지점별 발주 목록·지점 비활성화 시 진행 중 발주 확인(STORE_IN_USE), 창고별 상태 목록 조회
CREATE INDEX idx_store_order_store_status ON store_order (store_id, status);
CREATE INDEX idx_store_order_warehouse_status ON store_order (warehouse_id, status);
CREATE INDEX idx_store_order_status_requested_at ON store_order (status, requested_at);
CREATE INDEX idx_store_order_created_by ON store_order (created_by);

CREATE TABLE store_order_line (
    store_order_line_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_order_id              BIGINT        NOT NULL,
    sku_id                      BIGINT        NOT NULL,
    requested_quantity          BIGINT        NOT NULL,
    allocated_quantity          BIGINT        NOT NULL DEFAULT 0,
    shipped_quantity            BIGINT        NOT NULL DEFAULT 0,
    -- 등록 시점 ProductSKU.current_supply_price 스냅샷. 데이터 사전 기준으로 NULL을 허용하지만
    -- 도메인(StoreOrderLine.register)은 단가 없는 항목을 만들지 못하게 막는다(SUPPLY_PRICE_MISSING).
    requested_unit_supply_price DECIMAL(18,2) NULL,
    status                      VARCHAR(20)   NOT NULL DEFAULT 'REQUESTED',
    created_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_store_order_line_order_sku UNIQUE (store_order_id, sku_id),
    -- 도메인을 거치지 않는 경로로 수량·단가가 깨지지 않도록 DB에서도 한 번 더 막는다.
    CONSTRAINT ck_store_order_line_requested_quantity CHECK (requested_quantity > 0),
    CONSTRAINT ck_store_order_line_allocated_quantity CHECK (allocated_quantity >= 0),
    CONSTRAINT ck_store_order_line_shipped_quantity CHECK (shipped_quantity >= 0),
    CONSTRAINT ck_store_order_line_unit_supply_price CHECK (requested_unit_supply_price >= 0),
    CONSTRAINT fk_store_order_line_order FOREIGN KEY (store_order_id) REFERENCES store_order (store_order_id),
    CONSTRAINT fk_store_order_line_sku FOREIGN KEY (sku_id) REFERENCES product_sku (sku_id)
);

CREATE INDEX idx_store_order_line_sku_id ON store_order_line (sku_id);
