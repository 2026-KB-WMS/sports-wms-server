# common 코드 가이드

`src/main/java/com/kb/wms/common/`의 공통 코드. 모든 도메인이 사용하며, common은 도메인 패키지를 알지 못한다(의존은 항상 도메인 → common).

| 패키지 | 문서 | 한 줄 요약 |
|---|---|---|
| `config` | `config/Configs.md`, `config/SecurityConfig.md` | JPA Auditing, Swagger, 보안 필터 체인과 역할별 경로 규칙 |
| `exception` | `exception/Exceptions.md`, `exception/GlobalExceptionHandler.md` | 오류 코드 체계, `BusinessException`, 전역 예외 처리 |
| `response` | `response/Responses.md` | `ApiResponse`, `ErrorResponse`, `ItemsResponse` |
| `persistence` | `persistence/Persistence.md` | `BaseTimeEntity`, `SearchKeyword` |
| `security` | `security/AuthenticatedUser.md`, `security/Jwt.md` | 현재 사용자 컨텍스트, JWT 인증 흐름 |
| `validation` | `validation/Validation.md` | `PhoneNumber`, `GeoCoordinates` |
| `locationcode` | `locationcode/LocationCode.md` | 창고·지점 코드 자동 발급 |

## 읽는 순서 추천

1. `exception/Exceptions.md` → `response/Responses.md` : 모든 API의 성공·오류 응답 형태
2. `security/Jwt.md` → `security/AuthenticatedUser.md` → `config/SecurityConfig.md` : 인증·인가 구조
3. 나머지는 필요할 때
