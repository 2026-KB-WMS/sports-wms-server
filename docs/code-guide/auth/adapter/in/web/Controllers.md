# 컨트롤러

`auth/adapter/in/web/`

## AuthController

`/api/v1/auth` · Swagger 태그 "인증" · 의존: `AuthUseCase`, `UserUseCase`

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 | 허용 |
|---|---|---|---|---|---|---|
| `signUp` | POST `/signup` | 201 | `SignupRequest` | `SignupResponse` | 가입 신청 (PENDING) | **공개** |
| `login` | POST `/login` | 200 | `LoginRequest` | `LoginResponse` | 로그인, 토큰 발급 | **공개** |
| `getMe` | GET `/me` | 200 | - | `MyProfileResponse` | 내 정보. 조회 대상은 **토큰 주체 본인으로 고정** | 인증됨 |
| `changePassword` | PATCH `/me/password` | 200 | `ChangePasswordRequest` | `ApiResponse<Void>` ("비밀번호가 변경되었습니다.") | 비밀번호 변경. 대상은 토큰 주체 본인 | 인증됨 |

## UserController

`/api/v1/users` · Swagger 태그 "계정 관리" · 의존: `UserUseCase`, `UserManagementUseCase` · **HQ_ADMIN 전용**(`SecurityConfig`)

| 메서드 | 경로 | 상태 | 쿼리·바디 | 응답 | 설명 |
|---|---|---|---|---|---|
| `getUsers` | GET `/` | 200 | `role`, `status`, `keyword`, `page`, `size`, `sort` | `PageResponse<UserSummaryResponse>` | 사용자 목록(정렬 필드: `createdAt`, `name`, `loginId`, `role`, `status`). 가입 승인 대기는 `status=PENDING` |
| `updateUser` | PATCH `/{userId}` | 200 | `UserUpdateRequest` | `UserUpdateResponse` | 부분 수정. **승인·반려·비활성화·재활성화도 `status` 변경으로 처리**, 승인과 함께 소속 지정 가능 |

`updateUser`는 `UserUseCase`가 아니라 **`UserManagementUseCase`**를 호출해 승인과 소속 배정을 한 트랜잭션으로 처리한다. 요청 관리자의 ID(`principal.userId()`)를 `actorUserId`로 넘긴다.
