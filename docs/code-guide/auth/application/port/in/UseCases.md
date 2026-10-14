# 인바운드 포트 (UseCase)

`auth/application/port/in/`

| UseCase | 구현 | 호출자 | 메서드 |
|---|---|---|---|
| `AuthUseCase` | `AuthService` | `AuthController` | `login` |
| `UserUseCase` | `UserService` | `AuthController`, `UserController`, **창고·지점 도메인**, `InitialHqAdminRunner` | `signUp`, `getUser`, `getUsers`, `updateUser`, `changePassword`, `ensureInitialHqAdmin` |
| `UserManagementUseCase` | `UserManagementService` | `UserController` | `updateUser` (승인 + 소속 배정) |

`AuthenticatedUserResolver`(common의 인터페이스)는 `AuthenticatedUserService`가 구현하며 JWT 필터가 호출한다.

## 다른 도메인이 쓰는 방식

`WarehouseMemberService`, `StoreMemberService`가 `UserUseCase.getUser(userId)`로 대상 사용자의 존재·역할·상태를 확인한다. 이 의존 때문에 `UserService`가 배정 서비스를 직접 호출할 수 없고, 둘을 잇는 `UserManagementService`가 따로 있다.

## UserUseCase.updateUser 와 UserManagementUseCase.updateUser

| | `UserUseCase.updateUser` | `UserManagementUseCase.updateUser` |
|---|---|---|
| 상태·필드 변경 | ✔ | ✔ (위를 호출) |
| 소속 지정 요청 | 형식 검사만 | 검사 후 **같은 트랜잭션에서 배정** |
| 컨트롤러 사용 | 아니오 | **예** (`PATCH /users/{userId}`) |
