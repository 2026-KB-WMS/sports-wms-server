# 요청 DTO (Request)

`store/adapter/in/web/dto/request/` · 요청 바디 `record`.

- 형식·길이는 `@Valid`, 업무 규칙은 서비스에서 검증한다.
- 수정 요청에서 바꿀 수 없는 필드(`storeCode`, `isActive`)는 `toCommand()`가 값이 있으면 **400 `VALIDATION_ERROR`**를 던진다.
- 위도·경도는 `@AssertTrue isCoordinatePairValid`로 **쌍으로만** 받는다.

## StoreRegisterRequest

| 항목 | 내용 |
|---|---|
| 용도 | 지점 등록 요청 바디 |
| 엔드포인트 | POST `/api/v1/stores` |
| 변환 | `toCommand()` → `StoreRegisterCommand` |
| 필드 | storeName(필수, 최대 100), address(필수, 최대 500), contactName(최대 100), contactNumber(**필수**, 최대 30, `@PhoneNumber`), latitude(-90~90), longitude(-180~180) |
| 주의 | 지점 코드는 요청에 없다. 창고와 달리 **연락처가 필수**이고 수용량 필드가 없다 |

## StoreUpdateRequest

| 항목 | 내용 |
|---|---|
| 용도 | 지점 부분 수정 요청 바디 |
| 엔드포인트 | PATCH `/api/v1/stores/{storeId}` |
| 변환 | `toCommand()` → `StoreUpdateCommand` |
| 필드 | storeName, address, contactName, contactNumber, storeCode, isActive, latitude, longitude. 모두 선택 |
| 추가 검사 | `storeCode`나 `isActive`가 있으면 거부. 위도·경도는 쌍으로만 |
| 생성자 | 좌표 없는 6인자 생성자(테스트·호환용) |

## StoreMemberAssignRequest

| 항목 | 내용 |
|---|---|
| 용도 | 지점 관리자 배정 요청 바디 |
| 엔드포인트 | POST `/api/v1/stores/assign` |
| 변환 | `toCommand()` → `StoreMemberAssignCommand` |
| 필드 | storeId(필수), userId(필수), memberRole(필수) |

## StoreDeactivateRequest

| 항목 | 내용 |
|---|---|
| 용도 | 지점 비활성화 사유 |
| 엔드포인트 | PATCH `/api/v1/stores/{storeId}/deactivate` |
| 변환 | 없음. 컨트롤러가 `request.reason()`을 서비스에 직접 넘긴다 |
| 필드 | reason(선택, 최대 500) |
| 쓰임 | 바디와 reason 모두 선택. 값이 있으면 `StatusHistory.reason`에 저장 |
