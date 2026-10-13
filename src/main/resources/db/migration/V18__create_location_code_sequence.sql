-- 창고·지점 코드 자동 발급용 채번 테이블(ADR-021).
-- 코드 형식은 {WH|ST}-{시·도 2자}-{일련번호 3자리}이며, 키는 종류+시·도(예: WH-SU)다.
-- 다른 시·도로 코드가 재발급되어도 번호가 재사용되지 않도록 마지막 발급 번호를 별도로 보관한다.
-- 17개 시·도 × 2종류를 미리 채워 두어 동시 등록 시 행 생성 경합이 없게 한다.

CREATE TABLE location_code_sequence (
    sequence_key VARCHAR(20) NOT NULL,
    last_no      BIGINT      NOT NULL,
    CONSTRAINT pk_location_code_sequence PRIMARY KEY (sequence_key)
);

INSERT INTO location_code_sequence (sequence_key, last_no)
SELECT CONCAT(t.prefix, '-', r.region), 0
FROM (SELECT 'WH' AS prefix UNION ALL SELECT 'ST') t
CROSS JOIN (
    SELECT 'SU' AS region UNION ALL SELECT 'BS' UNION ALL SELECT 'DG' UNION ALL SELECT 'IC'
    UNION ALL SELECT 'GJ' UNION ALL SELECT 'DJ' UNION ALL SELECT 'US' UNION ALL SELECT 'SJ'
    UNION ALL SELECT 'GG' UNION ALL SELECT 'GW' UNION ALL SELECT 'CB' UNION ALL SELECT 'CN'
    UNION ALL SELECT 'JB' UNION ALL SELECT 'JN' UNION ALL SELECT 'GB' UNION ALL SELECT 'GN'
    UNION ALL SELECT 'JJ'
) r;
