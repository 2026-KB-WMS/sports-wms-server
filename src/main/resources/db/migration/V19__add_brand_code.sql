-- 브랜드 코드(영문 3자, ADR-023). 기존 브랜드에는 코드가 없으므로 NULL을 허용한다.
-- MySQL의 UNIQUE 인덱스는 NULL을 여러 개 허용하므로 코드가 없는 기존 브랜드끼리는 충돌하지 않는다.

ALTER TABLE brand ADD COLUMN brand_code VARCHAR(3) NULL AFTER brand_id;
ALTER TABLE brand ADD CONSTRAINT uk_brand_code UNIQUE (brand_code);
