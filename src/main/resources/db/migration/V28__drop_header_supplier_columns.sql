-- 공급처를 발주 헤더·입고 헤더가 아니라 공급처 발주서(supplier_order)로 옮기는 마무리 (이슈 #266, #269, ADR-030)
-- 서비스가 새 컬럼(purchase_order_line.supplier_id, supplier_order, inbound.supplier_order_id)만 쓰게 되었으므로
-- purchase_order.supplier_id와 inbound.supplier_id를 제거한다.
--
-- 제거 전에, V26·V27 적용 이후 이전 서비스로 등록된 데이터(라인에 공급처가 없고 헤더에만 있는 요청, 발주서가 없는
-- 확정 요청, 발주서에 연결되지 않은 입고)를 V27과 같은 규칙으로 보완한다. 새 발주서 번호는 같은 날짜의
-- 기존 번호 개수 뒤에서 이어 붙인다.

-- 1) 라인 공급처: 헤더 공급처로 채우고, 확정 시 매핑 검증이 걸리지 않도록 (SKU, 공급처) 매핑도 보완한다.
UPDATE purchase_order_line pol
    JOIN purchase_order po ON po.purchase_order_id = pol.purchase_order_id
SET pol.supplier_id = po.supplier_id
WHERE pol.supplier_id IS NULL
  AND po.supplier_id IS NOT NULL;

INSERT INTO sku_supplier (sku_id, supplier_id)
SELECT DISTINCT pol.sku_id, pol.supplier_id
FROM purchase_order_line pol
WHERE pol.supplier_id IS NOT NULL
  AND NOT EXISTS (SELECT 1
                  FROM sku_supplier ss
                  WHERE ss.sku_id = pol.sku_id AND ss.supplier_id = pol.supplier_id);

-- 2) 발주서가 없는 확정·완료·취소 요청의 (요청, 공급처) 쌍마다 발주서를 만든다.
INSERT INTO supplier_order (supplier_order_no, purchase_order_id, supplier_id, status, created_at, updated_at)
SELECT CONCAT('SO-', DATE_FORMAT(g.created_at, '%Y%m%d'), '-',
              LPAD(ROW_NUMBER() OVER (PARTITION BY DATE(g.created_at) ORDER BY g.purchase_order_id, g.supplier_id)
                       + (SELECT COUNT(*)
                          FROM supplier_order existing
                          WHERE existing.supplier_order_no LIKE
                                CONCAT('SO-', DATE_FORMAT(g.created_at, '%Y%m%d'), '-%')), 4, '0')),
       g.purchase_order_id, g.supplier_id, g.derived_status, g.created_at, g.updated_at
FROM (
    SELECT po.purchase_order_id, pol.supplier_id, po.created_at, po.updated_at,
           CASE
               WHEN po.status = 'CANCELED' THEN 'CANCELED'
               WHEN po.status = 'COMPLETED' THEN 'COMPLETED'
               WHEN SUM(pol.status <> 'COMPLETED') = 0 THEN 'COMPLETED'
               WHEN SUM(pol.received_quantity) > 0 THEN 'PARTIALLY_RECEIVED'
               ELSE 'ISSUED'
           END AS derived_status
    FROM purchase_order po
        JOIN purchase_order_line pol ON pol.purchase_order_id = po.purchase_order_id
    WHERE po.status IN ('CONFIRMED', 'COMPLETED', 'CANCELED')
      AND pol.supplier_id IS NOT NULL
      AND NOT EXISTS (SELECT 1
                      FROM supplier_order so
                      WHERE so.purchase_order_id = po.purchase_order_id AND so.supplier_id = pol.supplier_id)
    GROUP BY po.purchase_order_id, pol.supplier_id, po.created_at, po.updated_at, po.status
) g;

-- 3) 라인과 입고를 발주서에 연결한다.
UPDATE purchase_order_line pol
    JOIN supplier_order so ON so.purchase_order_id = pol.purchase_order_id AND so.supplier_id = pol.supplier_id
SET pol.supplier_order_id = so.supplier_order_id
WHERE pol.supplier_order_id IS NULL;

UPDATE inbound ib
    JOIN purchase_order po ON po.purchase_order_id = ib.purchase_order_id
    JOIN supplier_order so ON so.purchase_order_id = ib.purchase_order_id
                          AND so.supplier_id = COALESCE(ib.supplier_id, po.supplier_id)
SET ib.supplier_order_id = so.supplier_order_id
WHERE ib.supplier_order_id IS NULL;

-- 4) 헤더 공급처 컬럼 제거 (FK와 인덱스를 먼저 지운다).
ALTER TABLE purchase_order DROP FOREIGN KEY fk_purchase_order_supplier;
DROP INDEX idx_purchase_order_supplier_status ON purchase_order;
ALTER TABLE purchase_order DROP COLUMN supplier_id;

ALTER TABLE inbound DROP FOREIGN KEY fk_inbound_supplier;
DROP INDEX idx_inbound_supplier_id ON inbound;
ALTER TABLE inbound DROP COLUMN supplier_id;
