# 조회 결과 (Result)

`store/application/port/in/result/`

## CodeItem

| 항목 | 내용 |
|---|---|
| 용도 | 고정 코드 목록의 항목 하나 |
| 엔드포인트 | GET `/stores/management-types` |
| 만드는 곳 | `StoreCodeService` |
| 쓰는 곳 | `CodeItemResponse.from` |
| 필드 | code, name |

## StoreMemberView

| 항목 | 내용 |
|---|---|
| 용도 | 지점 관리자 배정 한 건과 대상 사용자의 이름·로그인 아이디 |
| 엔드포인트 | POST `/stores/assign`, GET `/stores/managers` |
| 만드는 곳 | 목록은 `StoreMemberJpaRepository.search`가 사용자 테이블을 ID로 조인해 채우고, 배정 직후에는 `StoreMemberService`가 `of(member, userName, loginId)`로 채운다 |
| 쓰는 곳 | `StoreMemberController.toResponse` → `StoreMemberResponse.of(view, storeName)` |
| 필드 | storeMemberId, storeId, userId, userName, loginId, memberRole, assignedAt |
| 쓰임 | 지점 이름은 포함하지 않는다. 웹 어댑터가 조회해 붙인다 |

## StoreMembershipSummary

| 항목 | 내용 |
|---|---|
| 용도 | 로그인한 점주 본인의 소속 지점 정보(지점 정보 + 배정 정보) |
| 엔드포인트 | GET `/stores/my` |
| 만드는 곳 | `StoreService.getMyStores` |
| 쓰는 곳 | `StoreMembershipResponse.from` |
| 필드 | storeId, storeCode, name, address, contactName, contactNumber, isActive, storeMemberId, memberRole, assignedAt |
