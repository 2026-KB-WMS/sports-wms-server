# store 코드 가이드

`src/main/java/com/kb/wms/store/`의 패키지 구조를 따라 클래스별 설명을 모았다.

## 도메인 개요

발주를 요청하는 **판매 지점**과 지점 관리자(점주) 배정을 관리한다. 지점은 재고를 갖지 않고, 삭제 없이 비활성화만 가능하다. `warehouse` 도메인과 구조가 거의 같지만 구역·수용량이 없고, 활성화·비활성화를 **상태 이력(`statushistory`)에 기록**한다.

## 읽는 순서 추천

1. `domain/entity/` → `domain/enums/`
2. `application/service/StoreService.md`, `StoreMemberService.md`
3. `adapter/in/web/`
4. `application/port/out/Repositories.md` → `adapter/out/`

## 문서 위치

| 패키지 | 문서 |
|---|---|
| `domain/entity` | Store, StoreMember |
| `domain/enums` | `Enums.md` (StoreStatus, StoreManagementType) |
| `exception` | `StoreErrorCode.md` |
| `application/service` | StoreService, StoreMemberService, StoreCodeService |
| `application/port/in` | `UseCases.md`, `command/Commands.md`, `query/SearchConditions.md`, `result/Results.md` |
| `application/port/out` | `Repositories.md` (StoreRepository, StoreMemberRepository, StoreOrderPresencePort) |
| `adapter/in/web` | 컨트롤러 3개, `dto/request/Requests.md`, `dto/response/Responses.md` |
| `adapter/out/storeorder` | `StoreOrderPresenceAdapter.md` |
| `adapter/out/persistence` | `PersistenceAdapters.md`, `repository/JpaRepositories.md`, `entity/JpaEntities.md` |

## 핵심 규칙

- **코드는 서버가 만든다**: 지점 코드는 주소의 시·도로 발급(`ST-BS-002`). 시·도가 바뀌는 주소 수정이면 재발급
- **비활성화 전 확인**: 진행 중인 지점 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)가 있으면 `STORE_IN_USE`
- **상태 이력**: 활성화·비활성화마다 `StatusHistory`에 처리자와 사유를 기록한다
- **점주 배정**: 사용자 역할이 `STORE_OWNER`이고 `ACTIVE`일 때만. 승인 전 소속은 가입 승인 요청에서 함께 지정
- **담당 지점 범위**는 서비스가 `AuthenticatedUser`로 검사한다(`getStore(id, actor)`)
