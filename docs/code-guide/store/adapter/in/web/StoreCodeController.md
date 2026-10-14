# StoreCodeController

`store/adapter/in/web/StoreCodeController.java` · `@RestController` · `/api/v1/stores` · Swagger 태그 "지점 코드"

## 의존

`StoreCodeUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|
| `getManagementTypes` | GET `/management-types` | 200 | `ItemsResponse<CodeItemResponse>` | 지점 담당 역할 코드 목록 (OWNER, MANAGER) | 인증된 모든 역할 |

`SecurityConfig`에서 `/stores/**` 기본 규칙(HQ_ADMIN)보다 위에 `authenticated()`로 열려 있다.
