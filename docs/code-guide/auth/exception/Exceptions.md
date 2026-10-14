# AuthErrorCode / AuthConstraintErrorCodes

`auth/exception/`

## AuthErrorCode

| 코드 | HTTP | 의미 | 발생 |
|---|---|---|---|
| USER_NOT_FOUND | 404 | 사용자를 찾을 수 없음 | 조회·수정·비밀번호 변경, 창고·지점 배정 |
| DUPLICATE_LOGIN_ID | 409 | 이미 사용 중인 로그인 아이디 | 가입, 최초 관리자 |
| DUPLICATE_EMAIL | 409 | 이미 가입된 이메일 | 가입, 수정, 최초 관리자 |
| INVALID_USER_STATUS_TRANSITION | 409 | 허용되지 않는 계정 상태 변경 | 계정 수정 |
| AFFILIATION_REQUIRED | 409 | 소속이 배정되지 않은 창고 관리자·점주는 활성화할 수 없음 | 승인·재활성화 |
| AFFILIATION_ASSIGNED | 409 | 소속이 있는 사용자의 역할 변경, 또는 이미 소속이 있는 사용자에게 소속 지정 | 계정 수정 |
| CURRENT_PASSWORD_MISMATCH | **400** | 현재 비밀번호가 틀림 | 비밀번호 변경 |
| ACCOUNT_PENDING | 403 | 가입 승인 대기 중인 계정 | 로그인 |
| ACCOUNT_INACTIVE | 403 | 비활성화된 계정 | 로그인, 비밀번호 변경 |

`CURRENT_PASSWORD_MISMATCH`를 401이 아닌 **400**으로 둔 이유: 클라이언트가 401을 "토큰 만료"로 해석해 로그아웃시키지 않도록 하기 위해서다.

## AuthConstraintErrorCodes

| 제약 이름 | 오류 코드 | 기준 |
|---|---|---|
| uk_users_login_id | DUPLICATE_LOGIN_ID | `V14__create_users_table.sql` |
| uk_users_email | DUPLICATE_EMAIL | 〃 |

## 공통 코드를 쓰는 경우

| 상황 | 코드 |
|---|---|
| 없는 아이디, 비밀번호 불일치 (같은 응답) | 401 `UNAUTHORIZED` |
| 입력 형식·변경 불가 필드·본인 계정 보호 | 400 `VALIDATION_ERROR` |
