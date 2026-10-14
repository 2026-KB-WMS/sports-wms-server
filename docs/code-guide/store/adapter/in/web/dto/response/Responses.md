# 응답 DTO (Response)

`store/adapter/in/web/dto/response/` · 응답 `record`. JSON 필드는 camelCase. 목록은 `ItemsResponse`(`data.items`).

## StoreResponse

| 항목 | 내용 |
|---|---|
| 용도 | 지점 한 건의 전체 정보. 등록·수정·활성화·비활성화·단건 조회에 공통 |
| 엔드포인트 | POST `/stores`(201), GET·PATCH `/stores/{id}`, PATCH `.../activate`, `.../deactivate` |
| 만드는 곳 | `StoreController` → `from(Store)` |
| 필드 | storeId, storeCode, storeName, address, latitude, longitude, contactName, contactNumber, isActive, createdAt, updatedAt |

## StoreSummaryResponse

| 항목 | 내용 |
|---|---|
| 용도 | 전체 지점 목록의 항목 |
| 엔드포인트 | GET `/stores` |
| 만드는 곳 | `StoreController.getStores` → `from(Store)` |
| 필드 | storeId, storeCode, storeName, address, latitude, longitude, contactName, contactNumber, isActive |

## StoreMembershipResponse

| 항목 | 내용 |
|---|---|
| 용도 | 로그인한 점주의 소속 지점 한 건(지점 정보 + 배정 정보) |
| 엔드포인트 | GET `/stores/my` |
| 만드는 곳 | `StoreController.getMyStores` → `from(StoreMembershipSummary)` |
| 필드 | storeId, storeCode, storeName, address, contactName, contactNumber, isActive, storeMemberId, memberRole, assignedAt |

## StoreMemberResponse

| 항목 | 내용 |
|---|---|
| 용도 | 지점 관리자 배정 한 건. 사용자 정보는 이름·로그인 아이디만(민감 정보 제외) |
| 엔드포인트 | POST `/stores/assign`, GET `/stores/managers` |
| 만드는 곳 | `StoreMemberController.toResponse` → `of(view, storeName)` |
| 필드 | storeMemberId, storeId, storeName, userId, userName, loginId, memberRole, assignedAt |

## StoreMemberReleaseResponse

| 항목 | 내용 |
|---|---|
| 용도 | 배정 해제 결과 |
| 엔드포인트 | DELETE `/stores/managers/{storeMemberId}` |
| 필드 | storeMemberId |

## CodeItemResponse

| 항목 | 내용 |
|---|---|
| 용도 | 고정 코드 목록 항목 |
| 엔드포인트 | GET `/stores/management-types` |
| 만드는 곳 | `StoreCodeController` → `from(CodeItem)` |
| 필드 | code, name |
