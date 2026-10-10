-- SKU 단가를 필수로 바꾸고(ADR-017), 단가 변경 이력 테이블을 만든다.
--
-- 기존에 단가가 NULL이던 SKU는 0으로 채운 뒤 NOT NULL로 바꾼다. 0은 발주 단가로도 통과하므로,
-- 적용 후 단가 수정 API(PATCH /products/skus/{skuId})로 실제 단가를 넣어야 한다.
-- 적용 전 확인 쿼리: SELECT sku_id, sku_code FROM product_sku WHERE current_purchase_price IS NULL OR current_supply_price IS NULL;

UPDATE product_sku SET current_purchase_price = 0 WHERE current_purchase_price IS NULL;
UPDATE product_sku SET current_supply_price = 0 WHERE current_supply_price IS NULL;

ALTER TABLE product_sku
    MODIFY COLUMN current_purchase_price DECIMAL(18,2) NOT NULL,
    MODIFY COLUMN current_supply_price   DECIMAL(18,2) NOT NULL;

-- 단가가 바뀐 수정마다 한 행. 등록 시점의 초기 단가는 product_sku 행 자체가 기록이라 남기지 않는다.
CREATE TABLE sku_price_history (
    sku_price_history_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id                  BIGINT         NOT NULL,
    previous_purchase_price DECIMAL(18,2)  NOT NULL,
    new_purchase_price      DECIMAL(18,2)  NOT NULL,
    previous_supply_price   DECIMAL(18,2)  NOT NULL,
    new_supply_price        DECIMAL(18,2)  NOT NULL,
    changed_by              BIGINT         NOT NULL,
    changed_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sku_price_history_sku FOREIGN KEY (sku_id) REFERENCES product_sku (sku_id),
    CONSTRAINT fk_sku_price_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (user_id)
);

-- SKU별 이력을 시간순으로 조회
CREATE INDEX idx_sku_price_history_sku ON sku_price_history (sku_id, changed_at);
CREATE INDEX idx_sku_price_history_changed_by ON sku_price_history (changed_by);
