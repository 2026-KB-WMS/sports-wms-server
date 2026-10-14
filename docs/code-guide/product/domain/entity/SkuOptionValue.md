# SkuOptionValue

`product/domain/entity/SkuOptionValue.java`

SKU와 옵션 값을 연결해 SKU 구성을 확정하는 매핑. 식별자는 (skuId, optionValueId) 복합키다(ADR-009).

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| skuId | Long | SKU ID (복합키) |
| optionValueId | Long | 옵션 값 ID (복합키) |
| createdAt | LocalDateTime | 생성 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `connect(skuId, optionValueId)` | SKU에 옵션 값을 연결하는 매핑 생성 |

`equals/hashCode`는 `skuId`, `optionValueId` 두 필드만 사용한다.
