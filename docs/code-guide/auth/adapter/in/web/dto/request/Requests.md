# 요청 DTO (Request)

`auth/adapter/in/web/dto/request/` · 형식·길이는 `@Valid`, 아이디·비밀번호 형식 규칙은 서비스가 검사한다.

## SignupRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | POST `/api/v1/auth/signup` |
| 변환 | `toCommand()` → `UserSignupCommand` |
| 필드 | loginId(필수, 최대 50), password(필수), name(필수, 최대 100), email(필수, 이메일 형식, 최대 255), phone(필수, 최대 30, `@PhoneNumber`), role(필수) |
| 주의 | 아이디(소문자·숫자·밑줄 4~20자)·비밀번호(8자 이상, 영문·숫자·특수문자 포함) 형식은 **서비스가 검사**. `role`은 HQ_ADMIN을 서비스가 거절 |

## LoginRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | POST `/api/v1/auth/login` |
| 변환 | `toCommand()` → `LoginCommand` |
| 필드 | loginId(필수), password(필수) |

## ChangePasswordRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | PATCH `/api/v1/auth/me/password` |
| 변환 | `toCommand(userId)` → `ChangePasswordCommand` |
| 필드 | currentPassword(필수), newPassword(필수). 새 비밀번호 형식은 서비스가 검사 |

## UserUpdateRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | PATCH `/api/v1/users/{userId}` |
| 변환 | `toCommand(userId, actorUserId)` → `UserUpdateCommand` |
| 필드 | name(최대 100), email(이메일 형식, 최대 255), phone(최대 30, `@PhoneNumber`), role, status, loginId, password, warehouseId, storeId. 모두 선택 |
| 공백 검증 | name·email·phone은 값을 보냈다면 **공백 문자만으로 이루어질 수 없다**(`@Pattern .*\S.*`). 생략(null)은 "수정 안 함"이라 통과 |
| 주의 | `loginId`·`password`는 수정 대상이 아니지만 **무시하지 않고 서비스가 400으로 거절**하도록 그대로 넘긴다. `warehouseId`·`storeId`는 가입 승인(재활성화)과 함께 소속을 지정할 때만 보낸다 |
