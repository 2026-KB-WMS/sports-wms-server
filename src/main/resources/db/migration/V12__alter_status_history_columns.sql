-- status_history를 ERD 데이터 사전에 맞춘다.
-- 기준: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a
-- 상태 이름은 모두 20자 이하이므로 VARCHAR(20)으로 줄여도 잘리지 않는다.
-- changed_by는 시스템 자동 전이도 트리거한 사용자를 기록하므로 NOT NULL이다.
-- User 테이블이 아직 없어 FK는 여전히 걸지 않는다.

ALTER TABLE status_history MODIFY COLUMN from_status VARCHAR(20) NULL;
ALTER TABLE status_history MODIFY COLUMN to_status VARCHAR(20) NOT NULL;
ALTER TABLE status_history MODIFY COLUMN changed_by BIGINT NOT NULL;
