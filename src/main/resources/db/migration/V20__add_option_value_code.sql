-- 옵션 값 코드(ADR-025). SKU 코드 자동 생성에 쓴다.
-- 기존 옵션 값에는 코드가 없으므로 NULL을 허용한다.
-- MySQL의 UNIQUE 인덱스는 NULL을 여러 개 허용하므로 코드가 없는 기존 값끼리는 충돌하지 않는다.

ALTER TABLE option_value ADD COLUMN option_code VARCHAR(10) NULL AFTER option_group_id;
ALTER TABLE option_value ADD CONSTRAINT uk_option_value_group_code UNIQUE (option_group_id, option_code);
