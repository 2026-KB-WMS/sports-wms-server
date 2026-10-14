# StatusHistory / StatusHistoryEntityType

`statushistory/domain/entity/StatusHistory.java`, `domain/enums/StatusHistoryEntityType.java`

업무 엔티티의 **상태가 바뀔 때마다 남기는 이력 한 건**. 한 번 기록하면 수정하지 않는다(추가 전용).

엔티티 유형마다 상태 enum이 달라 이전·이후 상태는 **enum 이름 문자열**로 저장한다.

## StatusHistory 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| statusHistoryId | Long | PK |
| entityType | StatusHistoryEntityType | 어떤 엔티티의 이력인지 |
| entityId | Long | 해당 엔티티의 PK (**다형 참조**, 연관관계 없이 ID만) |
| fromStatus | String | 이전 상태. **최초 생성 기록이면 null** |
| toStatus | String | 이후 상태 (필수) |
| reason | String | 사유. 빈 값은 null로 정리, 최대 500자 |
| changedBy | Long | 처리자 사용자 ID (필수) |
| changedAt | LocalDateTime | 변경 시각 |

## 상수·메서드

| 항목 | 설명 |
|---|---|
| `MAX_REASON_LENGTH = 500` | 사유 최대 길이. 지점 비활성화 등 호출 쪽 검증이 이 값을 쓴다 |
| `record(...)` (static) | 이력 생성. `entityType`, `entityId`, `toStatus`(공백 불가), `changedBy` 필수. 사유는 `strip`하고 빈 값이면 null, 500자 초과면 `IllegalArgumentException` |
| `isInitial()` | `fromStatus == null`인지(최초 생성 기록) |

**시스템 자동 전이**(예: 마지막 입고 완료로 발주가 자동 COMPLETED)도 처리자는 **전이를 일으킨 사용자**를 넣는다. 처리자가 없는 이력은 만들 수 없다.

## StatusHistoryEntityType

`entity_id`가 어느 테이블의 PK인지 결정하는 값.

| 값 | 대상 | 기록하는 곳 |
|---|---|---|
| USER | 사용자 | `auth`의 `UserService` |
| PURCHASE_ORDER | 창고 발주 | `inbound`의 `PurchaseOrderService`, `InboundCompleteService`(자동 완료) |
| INBOUND | 입고 | `inbound`의 `InboundService`, `InboundInspectService`, `InboundCompleteService` |
| STORE_ORDER | 지점 발주 | `storeorder`의 `StoreOrderService`, `StoreOrderFulfillmentService`(자동 완료) |
| STOCK_ALLOCATION | 재고 할당 | `outbound`의 `StockAllocationService`, `OutboundFulfillmentService`, `StoreOrderOutboundAdapter` |
| OUTBOUND | 출고 | `outbound`의 `OutboundService`, `OutboundFulfillmentService`, `StoreOrderOutboundAdapter` |
| STORE | 지점 | `store`의 `StoreService`(활성화·비활성화) |
| LOT | 로트 | `inventory`의 `LotService`(상태 변경) |

새 업무 엔티티의 이력을 남기려면 이 enum에 값을 추가한다.
