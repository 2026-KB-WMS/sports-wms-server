# 거점 코드 발급 (locationcode)

`common/locationcode/` · 창고·지점 코드를 서버가 자동 발급한다. 형식은 `접두어-시·도-일련번호3자리`.

| 예 | 의미 |
|---|---|
| `WH-SU-001` | 서울(SU) 창고 1번 |
| `ST-BS-002` | 부산(BS) 지점 2번 |

## 흐름

주소 → `RegionCode.fromAddress`(시·도 판별) → `LocationCodeIssuer.issue(type, region, exists)` → 채번 행 잠금 → 번호 증가 → 코드

## LocationCodeIssuer

| 항목 | 내용 |
|---|---|
| 용도 | 거점 코드 발급 `@Component` |
| 트랜잭션 | `issue`는 `Propagation.MANDATORY`. **호출자의 트랜잭션 안에서만** 실행된다 |
| 의존 | `LocationCodeSequenceRepository` |

| 메서드 | 설명 |
|---|---|
| `issue(type, region, exists)` | `{prefix}-{region}` 키의 채번 행을 비관적 락으로 읽고(없으면 `saveAndFlush`로 생성), `%s-%03d`로 코드를 만든다. `exists`가 true(과거에 직접 입력된 코드와 겹침)면 다음 번호로 넘어간다 |
| `regionOf(code)` (static) | 발급 형식(`^(WH\|ST)-([A-Z]{2})-\d{3,}$`)의 코드면 시·도를 돌려준다. 직접 입력한 옛 코드나 알 수 없는 시·도면 비어 있다 |

## LocationCodeSequence

| 항목 | 내용 |
|---|---|
| 용도 | 종류+시·도별 마지막 발급 번호를 저장하는 JPA 엔티티 |
| 테이블 | `location_code_sequence` |
| 필드 | `sequenceKey`(PK, 예 `WH-SU`, 길이 20), `lastNo` |
| 메서드 | `next()`: `lastNo`를 올려 반환 (package-private) |
| 이유 | 코드가 다른 시·도로 재발급되어도 번호가 되돌아가지 않도록, 기존 코드 개수가 아니라 이 값으로 채번한다 |

## LocationCodeSequenceRepository

package-private `JpaRepository`. `findByKeyForUpdate(sequenceKey)`가 `PESSIMISTIC_WRITE` 락으로 읽는다.

## LocationType

코드를 발급하는 거점 종류와 접두어.

| 값 | prefix |
|---|---|
| `WAREHOUSE` | `WH` |
| `STORE` | `ST` |

## RegionCode

코드에 들어가는 시·도 코드(영문 2자). 주소의 **첫 단어**(정식명·약칭)로 판별한다.

| 코드 | 시·도 | 코드 | 시·도 | 코드 | 시·도 |
|---|---|---|---|---|---|
| SU | 서울 | BS | 부산 | DG | 대구 |
| IC | 인천 | GJ | 광주 | DJ | 대전 |
| US | 울산 | SJ | 세종 | GG | 경기 |
| GW | 강원 | CB | 충북 | CN | 충남 |
| JB | 전북 | JN | 전남 | GB | 경북 |
| GN | 경남 | JJ | 제주 | | |

| 메서드 | 설명 |
|---|---|
| `findByAddress(address)` | 주소 첫 단어가 정식명·약칭과 일치하는 지역. 없으면 비어 있다 |
| `fromAddress(address)` | 위와 같지만 없으면 `VALIDATION_ERROR`(400) "주소는 시·도로 시작해야 합니다" |

예: `서울 강남구 테헤란로 1` → SU, `경기도 성남시 ...` → GG
