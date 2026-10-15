-- 창고 발주 요청과 공급처 발주서 분리 (이슈 #266, #275, ADR-030)
-- 1) 공급처 발주서 supplier_order 생성: 요청 확정 시 공급처당 한 건
-- 2) purchase_order_line.supplier_order_id, inbound.supplier_order_id 추가
-- 3) 기존 확정·완료·취소 요청을 공급처별 발주서로 옮기고 라인·입고를 연결
--
-- purchase_order.supplier_id와 inbound.supplier_id 제거는 서비스가 새 컬럼만 쓰게 된 뒤 별도 마이그레이션에서 한다.

CREATE TABLE supplier_order (
    supplier_order_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_order_no  VARCHAR(50) NOT NULL,
    purchase_order_id  BIGINT      NOT NULL,
    supplier_id        BIGINT      NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_supplier_order_no UNIQUE (supplier_order_no),
    CONSTRAINT uk_supplier_order_po_supplier UNIQUE (purchase_order_id, supplier_id),
    CONSTRAINT fk_supplier_order_purchase_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (purchase_order_id),
    CONSTRAINT fk_supplier_order_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id)
);

-- 공급처·상태 필터 목록 조회, 공급처 비활성화 시 진행 중 발주서 확인. purchase_order_id는 UNIQUE 인덱스의 선두 컬럼이다.
CREATE INDEX idx_supplier_order_supplier_status ON supplier_order (supplier_id, status);

ALTER TABLE purchase_order_line
    ADD COLUMN supplier_order_id BIGINT NULL AFTER supplier_id,
    ADD CONSTRAINT fk_purchase_order_line_supplier_order FOREIGN KEY (supplier_order_id) REFERENCES supplier_order (supplier_order_id);

CREATE INDEX idx_purchase_order_line_supplier_order_id ON purchase_order_line (supplier_order_id);

ALTER TABLE inbound
    ADD COLUMN supplier_order_id BIGINT NULL AFTER supplier_id,
    ADD CONSTRAINT fk_inbound_supplier_order FOREIGN KEY (supplier_order_id) REFERENCES supplier_order (supplier_order_id);

CREATE INDEX idx_inbound_supplier_order_id ON inbound (supplier_order_id);

-- 기존 요청 데이터 이전. 공급처가 배정된 확정·완료·취소 요청만 대상이며(요청·반려 상태는 아직 발주서가 없다),
-- 번호는 요청 등록일 기준 SO-yyyyMMdd-NNNN으로 매긴다.
-- 상태: 요청이 취소·완료면 그대로, 확정이면 공급처 라인 진행(전량 완료 / 입고 수량 있음 / 없음)에서 도출한다.
INSERT INTO supplier_order (supplier_order_no, purchase_order_id, supplier_id, status, created_at, updated_at)
SELECT CONCAT('SO-', DATE_FORMAT(g.created_at, '%Y%m%d'), '-',
              LPAD(ROW_NUMBER() OVER (PARTITION BY DATE(g.created_at) ORDER BY g.purchase_order_id, g.supplier_id), 4, '0')),
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
    GROUP BY po.purchase_order_id, pol.supplier_id, po.created_at, po.updated_at, po.status
) g;

UPDATE purchase_order_line pol
    JOIN supplier_order so ON so.purchase_order_id = pol.purchase_order_id AND so.supplier_id = pol.supplier_id
SET pol.supplier_order_id = so.supplier_order_id;

-- 입고의 공급처는 V26에서 헤더 공급처로 채웠지만, 이후 등록된 입고는 비어 있을 수 있어 헤더 공급처로 보완한다.
UPDATE inbound ib
    JOIN purchase_order po ON po.purchase_order_id = ib.purchase_order_id
    JOIN supplier_order so ON so.purchase_order_id = ib.purchase_order_id
                          AND so.supplier_id = COALESCE(ib.supplier_id, po.supplier_id)
SET ib.supplier_order_id = so.supplier_order_id;
