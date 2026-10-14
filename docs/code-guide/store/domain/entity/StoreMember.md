# StoreMember

`store/domain/entity/StoreMember.java`

사용자와 지점의 소속·발주 역할을 연결하는 매핑. 점주(STORE_OWNER)가 **배정된 지점만 조회**하도록 제한하는 데 쓴다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| storeMemberId | Long | PK |
| storeId | Long | 지점 |
| userId | Long | 사용자 (auth 도메인) |
| memberRole | String | 담당 역할 코드. 허용 값은 `StoreManagementType` (OWNER, MANAGER) |
| assignedAt | LocalDateTime | 배정 시각 |
| createdAt | LocalDateTime | 생성 시각 (`updated_at` 없음) |

## 메서드

| 메서드 | 설명 |
|---|---|
| `assign(storeId, userId, memberRole, assignedAt)` | 배정 매핑 생성 |

이 매핑이 `AuthenticatedUser.storeIds`(담당 지점 범위)의 근거가 된다. 같은 (지점, 사용자)는 유니크(`uk_store_member_store_user`)다.
