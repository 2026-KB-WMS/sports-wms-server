# StockAllocation

`outbound/domain/entity/StockAllocation.java`

**재고 할당.** 발주 항목 하나가 **특정 재고 행(구역 + 로트)에서 예약한 수량**이다. 한 발주 항목이 여러 재고 행에 나뉘어 여러 할당이 될 수 있다.

## 상태

```
ALLOCATED ──pick()──▶ PICKED     (피킹 완료, 재고 차감됨)
ALLOCATED ──release()──▶ RELEASED (할당 해제, 예약 재고 반환)
```

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| allocationId | Long | PK |
| storeOrderLineId | Long | 발주 항목 |
| inventoryLotId | Long | 예약한 재고 행 |
| allocatedQuantity | long | 할당(예약) 수량 |
| pickedQuantity | long | 실제 피킹 수량. 피킹 전 0 |
| status | AllocationStatus | |
| allocatedAt | LocalDateTime | 할당 일시 |
| releasedAt | LocalDateTime | 해제 일시 |
| allocatedBy | Long | 할당 처리자 |
| createdAt, updatedAt | LocalDateTime | |

## 메서드

| 메서드 | 설명 | 위반 시 |
|---|---|---|
| `allocate(storeOrderLineId, inventoryLotId, quantity, allocatedBy, allocatedAt)` | ALLOCATED로 생성. 항목·재고 행 필수, 수량 ≥ 1 | `IllegalArgumentException` |
| `release(releasedAt)` | ALLOCATED → RELEASED | `IllegalStateException` |
| `pick(pickedQuantity)` | ALLOCATED → PICKED. `0 <= picked <= allocated` | 상태는 `IllegalStateException`, 수량은 `IllegalArgumentException` |
| `shortageQuantity()` | `allocated - picked` (피킹 부족분) | |
| `isActive()` | ALLOCATED 여부 | |

## 발주 도메인과의 관계

발주(`storeorder`)는 항목의 `allocatedQuantity`(예약 중)·`shippedQuantity`(출고됨)를 갖고, 이 클래스는 **어느 재고 행에서** 예약했는지를 기록한다. 둘은 ID로만 연결된다(ADR-005).
