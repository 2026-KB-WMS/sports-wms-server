# 인바운드 포트 (UseCase)

`store/application/port/in/` · 상세 동작은 각 서비스 문서를 본다.

| UseCase | 구현 | 메서드 | 호출자 |
|---|---|---|---|
| `StoreUseCase` | `StoreService` | `registerStore`, `getStores`, `getStore`(2종), `updateStore`, `deactivateStore`, `activateStore`, `getMyStores` | `StoreController`, `StoreMemberController`(지점명), 다른 도메인(주체 없는 `getStore`) |
| `StoreMemberUseCase` | `StoreMemberService` | `assignManager`, `getManagers`, `releaseManager` | `StoreMemberController` |
| `StoreCodeUseCase` | `StoreCodeService` | `getManagementTypes` | `StoreCodeController` |

## 오버로드 규칙

`getStore(id)`는 내부 조회용(권한 검사 없음), `getStore(id, actor)`는 사용자 요청용(담당 지점이 아니면 403)이다.

## 시그니처 주의

`deactivateStore(storeId, reason, userId)`, `activateStore(storeId, userId)`는 `AuthenticatedUser`가 아니라 **처리자 ID(Long)**를 받는다. 상태 이력의 `changed_by`로 쓰기 위해서다.
