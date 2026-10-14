# InventoryLot

`inventory/domain/entity/InventoryLot.java`

**특정 구역(section)의 특정 로트(lot)**의 실재고·할당 수량을 관리하는 재고 원장. 재고의 가장 작은 단위다.

## 수량 규칙

`0 <= allocated <= onHand`, 가용 수량은 `onHand - allocated`.

업무 오류(가용 재고 부족 등)는 서비스가 `canAllocate`, `canDecrease` 등으로 먼저 검증해 도메인 오류 코드로 응답하고, 이 클래스의 예외는 검증 누락을 막는 **최후 방어선**이다(ADR-008).

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| inventoryLotId | Long | PK. API에서는 `inventoryId`로도 불린다 |
| sectionId | Long | 구역 (창고는 구역을 통해 결정) |
| lotId | Long | 로트 |
| onHandQuantity | Long | 보유 수량. null이면 0 |
| allocatedQuantity | Long | 할당(예약) 수량. null이면 0 |
| qualityStatus | QualityStatus | AVAILABLE / DEFECTIVE. null이면 AVAILABLE |
| lastCountedAt | LocalDateTime | 마지막 실사(조정) 시각 |
| createdAt, updatedAt | LocalDateTime | |

(구역, 로트) 조합은 유니크(`uk_inventory_lot_section_lot`)다.

## 메서드

| 메서드 | 설명 | 위반 시 |
|---|---|---|
| `open(sectionId, lotId, qualityStatus)` | 수량 0인 새 원장 생성 | |
| `availableQuantity()` | `onHand - allocated` | |
| `isDefective()` | 품질이 DEFECTIVE인지 | |
| `canAllocate(q)` | `q > 0`, 불량 아님, 가용 수량 ≥ q | |
| `canDecrease(q)` | `q > 0`, 가용 수량 ≥ q (할당된 수량은 못 줄임) | |
| `canRelease(q)` | `q > 0`, 할당 수량 ≥ q | |
| `canShip(q)` | `canRelease`와 같다 | |
| `increase(q)` | 보유 수량 증가 (입고, 조정 증가) | `IllegalArgumentException` (q ≤ 0) |
| `decrease(q)` | 보유 수량 감소 (조정 감소) | `IllegalStateException` |
| `allocate(q)` | 할당 수량 증가 (출고용 예약) | `IllegalStateException` |
| `release(q)` | 할당 수량 감소 (출고 취소, 부족분 해제) | `IllegalStateException` |
| `ship(q)` | 할당 수량과 보유 수량을 **함께** 차감 | `IllegalStateException` |
| `markCounted(at)` | 마지막 실사 시각 갱신 | |

수량 컬럼은 모두 `long`/`BIGINT`다(프로젝트 규칙).
