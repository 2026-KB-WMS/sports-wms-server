# UserManagementService

`auth/application/service/UserManagementService.java` · 구현: `UserManagementUseCase` · 클래스 전체 `@Transactional`

**계정 수정(승인)과 소속 배정을 한 트랜잭션으로 묶는 서비스**(ADR-022).

## 왜 따로 있나

창고·지점의 배정 서비스(`WarehouseMemberService`, `StoreMemberService`)는 사용자 조회를 위해 `UserUseCase`를 쓴다. `UserService`가 배정 서비스를 직접 부르면 **순환 의존**이 생기므로, 이 서비스가 둘을 이어 준다.

```
UserManagementService ──▶ UserUseCase (상태 변경)
                      └──▶ WarehouseMemberUseCase / StoreMemberUseCase (배정)
```

## 의존

`UserUseCase`, `WarehouseMemberUseCase`, `StoreMemberUseCase`

## updateUser(command)

| 순서 | 동작 |
|---|---|
| 1 | `userUseCase.updateUser(command)`로 **상태·필드를 먼저 변경**한다. 소속 지정 요청은 이 단계에서 형식만 검사한다 |
| 2 | `warehouseId`가 있으면 창고 관리자 배정(`WarehouseManagementType.MANAGER`), `storeId`가 있으면 점주 배정(`StoreManagementType.OWNER`) |

**ACTIVE로 먼저 바꾼 뒤 배정**해야 배정 서비스의 "ACTIVE 사용자만 배정 가능" 검사를 통과한다. 배정이 실패하면 예외로 같은 트랜잭션이 롤백돼 **상태 변경도 반영되지 않는다.**

## 호출

`PATCH /api/v1/users/{userId}`가 `UserUseCase`가 아니라 이 UseCase를 호출한다(`UserController.updateUser`).
