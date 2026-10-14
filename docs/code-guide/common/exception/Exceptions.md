# 예외·오류 코드 (exception)

`common/exception/` · `GlobalExceptionHandler`는 별도 문서(`GlobalExceptionHandler.md`)를 본다.

## 구조

```
ErrorCode (공통 6종, HTTP 상태 결정)
   ▲ 가진다
DomainErrorCode (인터페이스) ← ProductErrorCode, WarehouseErrorCode ... (도메인별 enum)
   ▲ 받는다
BusinessException ── 던지면 ──▶ GlobalExceptionHandler ──▶ ErrorResponse
```

HTTP 상태는 공통 `ErrorCode`가 정하고, 응답의 `errorCode` 문자열만 도메인 값으로 세분화한다.

## ErrorCode

공통 API 규칙의 기본 오류 코드 enum. 도메인 코드는 이 enum을 확장하지 않고 `DomainErrorCode`를 구현한 별도 enum으로 둔다.

| 값 | HTTP | 기본 메시지 |
|---|---|---|
| `VALIDATION_ERROR` | 400 | 입력값을 확인해주세요. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다. |
| `NOT_FOUND` | 404 | 요청한 리소스를 찾을 수 없습니다. |
| `CONFLICT` | 409 | 이미 존재하거나 충돌하는 요청입니다. |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류가 발생했습니다. |

| 메서드 | 설명 |
|---|---|
| `getStatus()` | HTTP 상태 코드 숫자 |
| `getDefaultMessage()` | 기본 메시지 |

## DomainErrorCode

도메인별 오류 enum이 구현하는 인터페이스.

| 메서드 | 설명 |
|---|---|
| `getErrorCode()` | 소속 공통 `ErrorCode`(HTTP 상태를 결정) |
| `getDefaultMessage()` | 기본 메시지 |
| `name()` | enum 이름. 응답의 `errorCode` 문자열이 된다 |

## BusinessException

서비스 계층에서 던지는 `RuntimeException`.

| 생성자 | 설명 |
|---|---|
| `(ErrorCode)` | 공통 코드와 기본 메시지 |
| `(ErrorCode, message)` | 공통 코드와 메시지 직접 지정 |
| `(DomainErrorCode)` | 도메인 코드. HTTP 상태는 소속 `ErrorCode`, 응답 `errorCode`는 도메인 코드 이름 |
| `(DomainErrorCode, message)` | 도메인 코드 + 메시지 직접 지정 (예: 누락 그룹명을 메시지에 넣을 때) |
| `(ErrorCode, message, customErrorCode)` | 위 모두의 기반 생성자 |

| 메서드 | 설명 |
|---|---|
| `getErrorCode()` | 공통 `ErrorCode` (HTTP 상태용) |
| `getErrorCodeName()` | 응답에 쓰는 코드 문자열. 도메인 코드가 있으면 그 이름, 없으면 `ErrorCode` 이름 |

## ConstraintErrorCodeProvider

DB 유니크 제약 이름 → 도메인 오류 코드 대응표를 제공하는 인터페이스.

| 메서드 | 설명 |
|---|---|
| `constraintErrorCodes()` | `제약 이름(소문자) → DomainErrorCode` 맵 |

- 각 도메인이 자기 제약만 `@Component` 빈으로 등록한다(예: `ProductConstraintErrorCodes`).
- 제약 이름은 Flyway 마이그레이션과 JPA `@UniqueConstraint` 이름을 같게 유지해야 한다.
- 사용처: `GlobalExceptionHandler.handleDataIntegrityViolation`.
