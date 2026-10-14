# WarehouseCodeController

`warehouse/adapter/in/web/WarehouseCodeController.java` · `@RestController` · `/api/v1/warehouses` · Swagger 태그 "창고 코드"

## 의존

`WarehouseCodeUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|
| `getManagementTypes` | GET `/management-types` | 200 | `ItemsResponse<CodeItemResponse>` | 창고 관리자 담당 역할 코드 목록 (MANAGER) | 인증된 모든 역할 |
| `getSectionTypes` | GET `/section-types` | 200 | `ItemsResponse<CodeItemResponse>` | 구역 유형 코드 목록 (ZONE, RACK, DEFECT) | 인증된 모든 역할 |

`/management-types`, `/section-types`는 `SecurityConfig`에서 `/warehouses/**` 기본 규칙(HQ_ADMIN)보다 위에 `authenticated()`로 열려 있다. 화면의 선택 목록을 채우는 용도다.
