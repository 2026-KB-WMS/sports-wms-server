# Lot

`inventory/domain/entity/Lot.java`

동일 제조·입고 묶음을 식별하는 추적 단위(로트) 마스터. **같은 로트는 동일 원가**를 가지며 입고 시점에 확정된다. 별도 생성 API 없이 입고 검수 트랜잭션 안에서 만들어진다(ADR-004).

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| lotId | Long | PK |
| skuId | Long | SKU |
| supplierId | Long | 공급처 |
| lotNumber | String | 로트 번호 |
| manufacturedDate | LocalDate | 제조일 (선택) |
| expiryDate | LocalDate | 유통기한 (선택) |
| status | LotStatus | 생성 시 null이면 AVAILABLE |
| unitCost | BigDecimal | 원가 |
| createdAt, updatedAt | LocalDateTime | |

(SKU, 공급처, 로트 번호)는 유니크(`uk_lot_sku_supplier_number`)다.

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(skuId, supplierId, lotNumber, manufacturedDate, expiryDate, unitCost)` | AVAILABLE로 생성 |
| `isAvailable()` | 상태가 AVAILABLE인지 |
| `isExpiredAt(baseDate)` | 기준일에 유통기한이 지났는지. 유통기한이 없으면 만료되지 않는다 |
| `canTransitionTo(target)` | 요청으로 바꿀 수 있는 전이인지. 아래 표 |
| `quarantine()`, `release()`, `dispose()`, `expire()` | 상태를 각각 QUARANTINED, AVAILABLE, DISPOSED, EXPIRED로 변경 |

## 상태 전이

| 현재 | 허용되는 목표 |
|---|---|
| AVAILABLE | QUARANTINED, DISPOSED |
| QUARANTINED | AVAILABLE, DISPOSED |
| EXPIRED | DISPOSED |
| DISPOSED | 없음 (되돌릴 수 없다) |

`EXPIRED`는 요청으로 지정할 수 없는 목표 상태다.

## 관찰

- `expire()`는 정의돼 있지만 `src/main` 어디에서도 호출되지 않는다. 즉 **코드상 EXPIRED 상태가 되는 경로가 없다**(유통기한이 지나도 상태는 AVAILABLE로 남는다). 만료를 처리하는 배치가 예정돼 있는지는 코드로 확인되지 않아 수정하지 않았다.
