# StoreMemberController

`store/adapter/in/web/StoreMemberController.java` · `@RestController` · `/api/v1/stores` · Swagger 태그 "지점 담당자"

## 의존

`StoreMemberUseCase`, `StoreUseCase`(응답에 지점명을 채우려고)

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| `assignManager` | POST `/assign` | 201 | `StoreMemberAssignRequest` | `StoreMemberResponse` | 지점 관리자(점주) 배정 |
| `getManagers` | GET `/managers` | 200 | 쿼리 `storeId`, `userId`, `keyword` (모두 선택) | `ItemsResponse<StoreMemberResponse>` | 배정 목록 |
| `releaseManager` | DELETE `/managers/{storeMemberId}` | 200 | - | `StoreMemberReleaseResponse` | 배정 해제 |

세 엔드포인트 모두 HQ_ADMIN 전용이다.

## 창고와의 경로 차이

배정 등록이 창고는 `POST /warehouses/managers`, 지점은 **`POST /stores/assign`**이다. 조회·삭제는 둘 다 `/managers`를 쓴다.

## 응답 조립

`toResponse(view)`가 `storeUseCase.getStoreView(storeId).name()`으로 지점명을 조회해 `StoreMemberResponse.of(view, storeName)`을 만든다. 목록에서는 배정마다 지점을 조회한다.
