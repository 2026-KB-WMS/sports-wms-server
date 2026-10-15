-- 창고 발주를 SKU·수량 요청 + 본사의 라인별 공급처 지정 흐름으로 바꾸기 위한 스키마 변경 (이슈 #266, #267)
-- 1) SKU↔공급처 다대다 매핑 sku_supplier 생성
-- 2) 공급처를 발주 라인(purchase_order_line)으로 확장하고 기존 발주 데이터를 복사
-- 3) 요청 시점에는 공급처가 없으므로 purchase_order.supplier_id를 NULL 허용으로 완화
-- 4) 입고(inbound)에 공급처 컬럼 추가
--
-- purchase_order.supplier_id 컬럼 제거와 inbound.supplier_id의 NOT NULL 강화는
-- 서비스·어댑터가 새 컬럼을 쓰도록 바뀐 뒤 별도 마이그레이션에서 한다(레이어별 PR 사이에 빌드가 깨지지 않도록).

CREATE TABLE sku_supplier (
    sku_supplier_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id           BIGINT   NOT NULL,
    supplier_id      BIGINT   NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sku_supplier_sku_supplier UNIQUE (sku_id, supplier_id),
    CONSTRAINT fk_sku_supplier_sku FOREIGN KEY (sku_id) REFERENCES product_sku (sku_id),
    CONSTRAINT fk_sku_supplier_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id)
);

-- 공급처 기준 매핑 조회(공급처 상세, 공급처 사용 여부 확인). sku_id는 UNIQUE 인덱스의 선두 컬럼이라 따로 두지 않는다.
CREATE INDEX idx_sku_supplier_supplier_id ON sku_supplier (supplier_id);

ALTER TABLE purchase_order_line
    ADD COLUMN supplier_id BIGINT NULL AFTER sku_id,
    ADD CONSTRAINT fk_purchase_order_line_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id);

CREATE INDEX idx_purchase_order_line_supplier_id ON purchase_order_line (supplier_id);

-- 기존 발주는 헤더의 공급처가 곧 모든 라인의 공급처다.
UPDATE purchase_order_line pol
    JOIN purchase_order po ON po.purchase_order_id = pol.purchase_order_id
SET pol.supplier_id = po.supplier_id;

-- 확정 시 매핑된 공급처만 지정할 수 있으므로, 기존 발주에서 쓰인 (SKU, 공급처) 조합을 매핑으로 옮겨 둔다.
-- 이렇게 하지 않으면 이미 공급처가 정해진 요청 상태 발주가 확정 단계에서 매핑 검증에 걸린다.
INSERT INTO sku_supplier (sku_id, supplier_id)
SELECT DISTINCT sku_id, supplier_id
FROM purchase_order_line
WHERE supplier_id IS NOT NULL;

ALTER TABLE purchase_order
    MODIFY COLUMN supplier_id BIGINT NULL;

ALTER TABLE inbound
    ADD COLUMN supplier_id BIGINT NULL AFTER purchase_order_id,
    ADD CONSTRAINT fk_inbound_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id);

CREATE INDEX idx_inbound_supplier_id ON inbound (supplier_id);

UPDATE inbound ib
    JOIN purchase_order po ON po.purchase_order_id = ib.purchase_order_id
SET ib.supplier_id = po.supplier_id;
