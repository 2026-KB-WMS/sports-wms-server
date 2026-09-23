-- ERD 결정 사항: 수량 컬럼은 모두 BIGINT. V2에서 DECIMAL(14,3)로 생성된 안전 재고 수량을 맞춘다.
-- (이미 적용된 V2를 수정하면 Flyway 체크섬이 깨지므로 별도 마이그레이션으로 변경)
ALTER TABLE product_sku
    MODIFY COLUMN safety_stock_quantity BIGINT NOT NULL DEFAULT 0;
