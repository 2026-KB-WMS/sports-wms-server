# InitialHqAdminRunner

`auth/adapter/in/startup/InitialHqAdminRunner.java` · `@Component`, `ApplicationRunner`

앱 시작 시 **본사 관리자가 없으면 설정으로 첫 관리자를 만든다**(ADR-013). 본사 관리자는 가입 API로 만들 수 없고 가입 승인·계정 관리를 하려면 최소 한 명이 필요하기 때문이다.

## 설정

| 설정 키 | 환경 변수 | 기본값 |
|---|---|---|
| `wms.admin.login-id` | `WMS_ADMIN_LOGIN_ID` | 빈 문자열 |
| `wms.admin.password` | `WMS_ADMIN_PASSWORD` | 빈 문자열 |
| `wms.admin.name` | `WMS_ADMIN_NAME` | 빈 문자열 |
| `wms.admin.email` | `WMS_ADMIN_EMAIL` | 빈 문자열 |
| `wms.admin.phone` | `WMS_ADMIN_PHONE` | 빈 문자열 |

## 동작 (`run`)

`userUseCase.ensureInitialHqAdmin(command)`의 결과로 로그를 남긴다.

| 결과 | 로그 |
|---|---|
| CREATED | info: 최초 본사 관리자를 생성했습니다 (loginId만, **비밀번호는 로그에 남기지 않음**) |
| ALREADY_EXISTS | debug |
| NOT_CONFIGURED | warn: 설정하거나 DB에 직접 본사 관리자를 만들어야 한다는 안내 |

## 예외 처리

**기동을 막지 않는다.**

| 예외 | 처리 |
|---|---|
| `BusinessException` (설정 값 오류 등) | error 로그만 |
| `DataIntegrityViolationException` | 서버가 동시에 여러 대 떠서 **다른 인스턴스가 먼저 만든 경우**(유니크 제약). info 로그만 |
