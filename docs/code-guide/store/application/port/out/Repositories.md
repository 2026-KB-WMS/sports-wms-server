# 아웃바운드 포트

`store/application/port/out/`

## StoreRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById`, `existsById`, `existsByStoreCode` | 기본 |
| `search(condition, page)` | `PageResult` 반환. 조건이 null이면 무시. 정렬은 페이지 조건을 따른다(기본 생성 일시 내림차순) |

창고와 달리 `findByIdForUpdate`(비관적 락)가 없다. 지점에는 구역 수용량 같은 잠가야 하는 합계 검증이 없다.

## StoreMemberRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById`, `deleteById` | 기본 |
| `search(storeId, userId, keyword, page)` | 사용자의 이름·로그인 아이디를 붙인 `StoreMemberView`의 `PageResult`. keyword는 이름·로그인 아이디 부분 일치. 정렬은 페이지 조건을 따른다(기본 **배정 일시 내림차순**) |
| `findByUserId(userId)` | 사용자가 배정된 지점 소속 목록. 배정 일시 내림차순 |
| `existsByStoreIdAndUserId` | 중복 배정 확인 |

## StoreOrderPresencePort

지점 발주 도메인의 진행 중 발주 존재 여부(읽기). 지점 비활성화 전에 확인한다.

| 메서드 | 설명 |
|---|---|
| `hasInProgressOrders(storeId)` | 진행 중 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)가 있으면 true |

구현: `adapter/out/storeorder/StoreOrderPresenceAdapter`.
