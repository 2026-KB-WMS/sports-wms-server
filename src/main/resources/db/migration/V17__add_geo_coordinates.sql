-- 주소를 입력받는 창고·지점·공급처에 위도·경도를 저장한다(ADR-020).
--
-- 기존 행에는 좌표가 없으므로 NULL을 허용한다. 지도 API로 주소를 다시 입력할 때 함께 채워진다.
-- DECIMAL(10,7)은 위도(-90~90)와 경도(-180~180)를 소수 7자리(약 1cm)까지 담는다.

ALTER TABLE warehouse ADD COLUMN latitude  DECIMAL(10,7) NULL AFTER address;
ALTER TABLE warehouse ADD COLUMN longitude DECIMAL(10,7) NULL AFTER latitude;

ALTER TABLE store ADD COLUMN latitude  DECIMAL(10,7) NULL AFTER address;
ALTER TABLE store ADD COLUMN longitude DECIMAL(10,7) NULL AFTER latitude;

ALTER TABLE supplier ADD COLUMN latitude  DECIMAL(10,7) NULL AFTER address;
ALTER TABLE supplier ADD COLUMN longitude DECIMAL(10,7) NULL AFTER latitude;
