# Store

`store/domain/entity/Store.java`

상품을 요청(발주)하는 판매 지점 마스터. **재고를 갖지 않는 독립 도메인**이며, 삭제 없이 비활성화(ACTIVE → INACTIVE)만 허용한다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| storeId | Long | PK |
| storeCode | String | 지점 코드. 서버가 주소의 시·도로 발급(예: `ST-BS-002`) |
| name | String | 지점명 |
| address | String | 주소. 시·도로 시작해야 한다 |
| latitude, longitude | BigDecimal | 위도·경도. 소수 7자리로 반올림. null 허용 |
| contactName | String | 지점 담당자명 (선택) |
| contactNumber | String | 지점 연락처 (필수) |
| status | StoreStatus | ACTIVE / INACTIVE. 생성 시 null이면 ACTIVE |
| createdAt, updatedAt | LocalDateTime | 생성·수정 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(code, name, address, contactName, contactNumber)` | ACTIVE로 생성. 좌표는 `changeCoordinates`로 따로 넣는다 |
| `reissueCode(code)` | 주소의 시·도가 바뀌어 코드를 **다시 발급할 때만** 사용 |
| `changeName`, `changeAddress`, `changeContactName`, `changeContactNumber` | 필드 변경 |
| `changeCoordinates(lat, lon)` | 소수 7자리로 맞춰 쌍으로 변경 |
| `deactivate()` | 이미 비활성이면 `IllegalStateException`(개발자용 가드, ADR-008). 서비스가 먼저 검사해 409로 바꾼다 |
| `activate()` | 이미 활성이면 `IllegalStateException` |
| `isActive()` | ACTIVE 여부 |

창고(`Warehouse`)와 달리 활성화·비활성화 메서드가 **중복 호출을 예외로 막는다.**
