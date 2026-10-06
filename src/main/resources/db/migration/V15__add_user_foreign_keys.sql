-- users(V14) 테이블이 생겨서, 그동안 인덱스만 두고 미뤄 둔 사용자 참조 컬럼에 FK를 추가한다.
-- 대상: 소속(warehouse_member, store_member), 작성·처리자(created_by, received_by, allocated_by, shipped_by,
-- status_history.changed_by). 컬럼 타입(BIGINT)과 인덱스는 각 테이블 생성 마이그레이션에 이미 있다.
--
-- 주의: 기존 행의 사용자 ID가 users에 없으면 FK 추가가 실패한다(목업 시드 등).
-- 이 마이그레이션은 데이터를 지우지 않으므로, 적용 전에 해당 행을 정리하거나 해당 사용자를 먼저 만들어 둬야 한다.
-- MySQL의 DDL은 트랜잭션이 아니라서 중간에 실패하면 앞선 FK만 적용된 채 남고 Flyway 이력도 실패로 기록된다.
-- 그러면 정리한 뒤 `flyway repair`로 이력을 고치고, 이미 걸린 FK는 지운 다음 다시 적용해야 한다.
--
-- 적용 전 확인 쿼리: 각 줄의 건수가 모두 0이어야 한다.
--   SELECT 'warehouse_member.user_id', COUNT(*) FROM warehouse_member t LEFT JOIN users u ON u.user_id = t.user_id WHERE u.user_id IS NULL
--   UNION ALL SELECT 'store_member.user_id', COUNT(*) FROM store_member t LEFT JOIN users u ON u.user_id = t.user_id WHERE u.user_id IS NULL
--   UNION ALL SELECT 'inventory_transaction.created_by', COUNT(*) FROM inventory_transaction t LEFT JOIN users u ON u.user_id = t.created_by WHERE u.user_id IS NULL
--   UNION ALL SELECT 'purchase_order.created_by', COUNT(*) FROM purchase_order t LEFT JOIN users u ON u.user_id = t.created_by WHERE u.user_id IS NULL
--   UNION ALL SELECT 'inbound.received_by', COUNT(*) FROM inbound t LEFT JOIN users u ON u.user_id = t.received_by WHERE t.received_by IS NOT NULL AND u.user_id IS NULL
--   UNION ALL SELECT 'inbound_line.received_by', COUNT(*) FROM inbound_line t LEFT JOIN users u ON u.user_id = t.received_by WHERE u.user_id IS NULL
--   UNION ALL SELECT 'store_order.created_by', COUNT(*) FROM store_order t LEFT JOIN users u ON u.user_id = t.created_by WHERE u.user_id IS NULL
--   UNION ALL SELECT 'stock_allocation.allocated_by', COUNT(*) FROM stock_allocation t LEFT JOIN users u ON u.user_id = t.allocated_by WHERE t.allocated_by IS NOT NULL AND u.user_id IS NULL
--   UNION ALL SELECT 'outbound.shipped_by', COUNT(*) FROM outbound t LEFT JOIN users u ON u.user_id = t.shipped_by WHERE t.shipped_by IS NOT NULL AND u.user_id IS NULL
--   UNION ALL SELECT 'status_history.changed_by', COUNT(*) FROM status_history t LEFT JOIN users u ON u.user_id = t.changed_by WHERE u.user_id IS NULL;

ALTER TABLE warehouse_member
    ADD CONSTRAINT fk_warehouse_member_user FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE store_member
    ADD CONSTRAINT fk_store_member_user FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE inventory_transaction
    ADD CONSTRAINT fk_inventory_transaction_created_by FOREIGN KEY (created_by) REFERENCES users (user_id);

ALTER TABLE purchase_order
    ADD CONSTRAINT fk_purchase_order_created_by FOREIGN KEY (created_by) REFERENCES users (user_id);

ALTER TABLE inbound
    ADD CONSTRAINT fk_inbound_received_by FOREIGN KEY (received_by) REFERENCES users (user_id);

ALTER TABLE inbound_line
    ADD CONSTRAINT fk_inbound_line_received_by FOREIGN KEY (received_by) REFERENCES users (user_id);

ALTER TABLE store_order
    ADD CONSTRAINT fk_store_order_created_by FOREIGN KEY (created_by) REFERENCES users (user_id);

ALTER TABLE stock_allocation
    ADD CONSTRAINT fk_stock_allocation_allocated_by FOREIGN KEY (allocated_by) REFERENCES users (user_id);

ALTER TABLE outbound
    ADD CONSTRAINT fk_outbound_shipped_by FOREIGN KEY (shipped_by) REFERENCES users (user_id);

ALTER TABLE status_history
    ADD CONSTRAINT fk_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (user_id);
