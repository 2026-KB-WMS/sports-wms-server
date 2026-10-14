# 응답 DTO (Response)

`auth/adapter/in/web/dto/response/` · 모두 `User`에서 변환한다. **비밀번호 해시는 어떤 응답에도 포함하지 않는다.**

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `SignupResponse` | 가입 결과 | POST `/auth/signup` (201) | `of(User)` | userId, loginId, name, email, phone, role, status, createdAt |
| `LoginResponse` | 로그인 결과 | POST `/auth/login` | `of(LoginResult)` | accessToken, **tokenType**(`Bearer`), expiresIn, `user`(userId, loginId, name, role, status) |
| `MyProfileResponse` | 내 정보 | GET `/auth/me` | `of(User)` | userId, loginId, name, email, phone, role, status, lastLoginAt |
| `UserSummaryResponse` | 사용자 목록 항목 | GET `/users` | `of(User)` | userId, loginId, name, email, phone, role, status, lastLoginAt, createdAt |
| `UserUpdateResponse` | 수정 결과 | PATCH `/users/{id}` | `of(User)` | userId, loginId, name, email, phone, role, status, updatedAt |

`tokenType`은 상수 `"Bearer"`다. 클라이언트는 `Authorization: Bearer {accessToken}`으로 토큰을 보낸다.
