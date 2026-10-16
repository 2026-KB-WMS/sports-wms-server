# StoreController

`store/adapter/in/web/StoreController.java` · `@RestController` · `/api/v1/stores` · Swagger 태그 "지점"

## 의존

`StoreUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|---|
| `registerStore` | POST `/` | 201 | `StoreRegisterRequest` | `StoreResponse` | 지점 등록 | HQ_ADMIN |
| `getStores` | GET `/` | 200 | 쿼리 `keyword`, `isActive`, `page`, `size`, `sort` | `PageResponse<StoreSummaryResponse>` | 전체 지점 목록(정렬 필드: `createdAt`, `storeName`, `storeCode`, `isActive`) | HQ_ADMIN |
| `getMyStores` | GET `/my` | 200 | - | `ItemsResponse<StoreMembershipResponse>` | 로그인한 점주의 소속 지점 | STORE_OWNER |
| `getStore` | GET `/{storeId}` | 200 | - | `StoreResponse` | 단건 조회 | HQ_ADMIN, STORE_OWNER |
| `updateStore` | PATCH `/{storeId}` | 200 | `StoreUpdateRequest` | `StoreResponse` | 부분 수정 | HQ_ADMIN |
| `deactivateStore` | PATCH `/{storeId}/deactivate` | 200 | `StoreDeactivateRequest`(선택) | `StoreResponse` | 비활성화 + 이력 기록 | HQ_ADMIN |
| `activateStore` | PATCH `/{storeId}/activate` | 200 | - | `StoreResponse` | 활성화 + 이력 기록 | HQ_ADMIN |

- `/my`, 비활성화·활성화 처리자는 **토큰 주체**(`principal.userId()`)다.
- 단건 조회의 담당 지점 범위 검사는 서비스(`getStore(id, actor)`)가 한다.
- 비활성화 요청 바디는 `@RequestBody(required = false)`라서 **바디 없이 호출해도 된다**(reason은 null).
