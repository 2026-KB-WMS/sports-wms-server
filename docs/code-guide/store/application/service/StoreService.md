# StoreService

`store/application/service/StoreService.java` · 구현: `StoreUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

지점 등록·조회·수정·활성화·비활성화와 내 지점 조회.

## 의존

`StoreRepository`, `StoreMemberRepository`, `StoreOrderPresencePort`, `StatusHistoryUseCase`, `LocationCodeIssuer`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerStore(command)` | 쓰기 | 주소의 시·도 판별(실패 시 400) → 지점 코드 발급 → 생성 → 좌표 반영 → 저장. 점주 배정은 포함하지 않는다 |
| `getStores(condition)` | 읽기 | 검색 |
| `getStore(id)` | 읽기 | 단건. 없으면 `STORE_NOT_FOUND`. 내부 조회용 |
| `getStore(id, actor)` | 읽기 | `actor.requireStoreAccess(id)` 후 조회. 담당 지점이 아니면 403 |
| `updateStore(id, command)` | 쓰기 | 아래 규칙 |
| `deactivateStore(id, reason, userId)` | 쓰기 | 아래 규칙 |
| `activateStore(id, userId)` | 쓰기 | 아래 규칙 |
| `getMyStores(userId)` | 읽기 | 사용자의 배정 목록을 지점 정보와 합쳐 `StoreMembershipSummary`로 반환. 배정이 없으면 **빈 목록**(오류 아님) |

## updateStore 규칙

변경 필드가 없으면 `VALIDATION_ERROR`.

| 필드 | 규칙 |
|---|---|
| name, contactName, contactNumber | 값이 있으면 변경 |
| address | 시·도 판별(실패 시 400) 후 변경. **발급 형식 코드(`ST-SU-001`)를 가진 지점이 다른 시·도로 옮기면 새 시·도의 다음 번호로 재발급**. 같은 시·도 수정이나 직접 입력된 옛 코드는 유지 |
| latitude, longitude | **둘 다** 있을 때만 변경 |

## 비활성화 / 활성화

둘 다 처리자 `userId`가 필수(null이면 `VALIDATION_ERROR`)이고, **상태 이력(`StatusHistory`)에 기록**한다. 창고 도메인에는 이 이력이 없다.

| 동작 | 규칙 |
|---|---|
| `deactivateStore` | 사유가 있으면 공백 제거 후 `StatusHistory.MAX_REASON_LENGTH`(500자) 초과 시 `VALIDATION_ERROR`. 지점 없음 `STORE_NOT_FOUND`. 이미 비활성 409. **진행 중인 지점 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)가 있으면 `STORE_IN_USE`**. 종결된 발주(COMPLETED·CANCELED·REJECTED)는 막지 않는다. 비활성화 후 `STORE: ACTIVE → INACTIVE, reason, userId`를 기록 |
| `activateStore` | 이미 활성이면 409. 활성화 후 `INACTIVE → ACTIVE`(사유 없음)를 기록 |

비활성 지점의 점주 배정은 막힌다(`StoreMemberService`: 비활성 지점에는 관리자 배정 불가).
