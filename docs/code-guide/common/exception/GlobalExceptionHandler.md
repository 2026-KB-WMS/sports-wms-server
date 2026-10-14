# GlobalExceptionHandler

`common/exception/GlobalExceptionHandler.java` · `@RestControllerAdvice`

컨트롤러·서비스에서 발생한 예외를 공통 오류 응답(`ErrorResponse`)으로 바꾼다.

## 처리 목록

| 예외 | 응답 | 설명 |
|---|---|---|
| `BusinessException` | `errorCode`의 HTTP 상태 | 도메인 오류. `getErrorCodeName()`이 응답 `errorCode`가 된다(도메인 코드 우선) |
| `MethodArgumentNotValidException` | 400 `VALIDATION_ERROR` | `@Valid` 실패. 필드별 `errors`(field, reason) 목록 포함 |
| `MethodArgumentTypeMismatchException` | 400 | 쿼리·경로 파라미터 타입 불일치. `"{이름}의 형식이 올바르지 않습니다."` |
| `MissingServletRequestParameterException` | 400 | 필수 쿼리 파라미터 누락 |
| `HttpMessageNotReadableException` | 400 | 바디가 없거나 JSON 형식·타입(잘못된 enum 값 포함)이 틀림 |
| `DataIntegrityViolationException` | 409 | 아래 참고 |
| `PessimisticLockingFailureException` | 409 `CONFLICT` | 잠금 대기 초과·교착. 재시도하면 성공할 수 있다 |
| `NoResourceFoundException` | 404 `NOT_FOUND` | 존재하지 않는 경로 |
| `Exception` | 500 `INTERNAL_ERROR` | 그 밖의 모든 예외. 스택 트레이스는 로그에만 남긴다 |

## DataIntegrityViolationException 처리

서비스의 사전 중복 검사를 **동시 요청이 통과해 DB 제약에서 막힌 경우**다.

1. 원인 예외 메시지를 소문자로 바꿔 모든 `ConstraintErrorCodeProvider` 빈의 제약 이름과 비교한다.
2. 이름이 다른 이름을 포함하는 경우를 대비해 **긴 이름부터** 비교한다.
3. 맞는 제약이 있으면 그 도메인 오류 코드(예: `DUPLICATE_BRAND_NAME`)로 응답한다. 사전 검사와 같은 응답이 된다.
4. 없으면 로그를 남기고 일반 409 `CONFLICT`("데이터 제약 조건과 충돌하는 요청입니다...")로 응답한다.

## 의존

`ObjectProvider<ConstraintErrorCodeProvider>`: 각 도메인이 등록한 제약 대응표(`ProductConstraintErrorCodes` 등)를 순서대로 모은다. common이 도메인 패키지를 직접 알지 않게 하려는 구조다.
