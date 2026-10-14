# ProductSku

`product/domain/entity/ProductSku.java`

옵션까지 확정된 실제 재고·입출고 관리 단위(SKU).

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| skuId | Long | PK |
| productId | Long | 상품 ID |
| skuCode | String | SKU 코드 (유니크). 서버 자동 발급 |
| barcode | String | 바코드 (유니크) |
| name | String | SKU명 |
| weight | BigDecimal | 무게 |
| currentPurchasePrice | BigDecimal | 현재 매입 단가 (0 이상) |
| currentSupplyPrice | BigDecimal | 현재 공급 단가 (0 이상) |
| unit | String | 수량 단위. null이면 `EA`. 허용 값은 `SkuUnit` |
| safetyStockQuantity | Long | 안전 재고. null이면 0 |
| status | ProductStatus | 생성 시 null이면 ACTIVE |
| createdAt, updatedAt | LocalDateTime | 생성·수정 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(productId, skuCode, barcode, name, weight, purchasePrice, supplyPrice, unit, safetyStockQuantity)` | 단가 검증 후 ACTIVE로 생성. 단가가 null이거나 음수면 `IllegalArgumentException` |
| `update(name, barcode, weight, purchasePrice, supplyPrice, safetyStockQuantity)` | **null이 아닌 값만** 반영하는 부분 수정. 단가는 0 이상 검증. skuCode·productId·unit·상태는 여기서 못 바꾼다 |
| `isPurchaseInfoVisibleTo(isStoreOwner)` | 점주(STORE_OWNER)에게는 매입 단가·안전 재고를 숨긴다. 점주면 false |
| `activate()` / `deactivate()` / `isActive()` | 상태 변경·조회 |

단가 변경 이력은 `SkuPriceHistory`에 따로 남긴다(서비스 담당).
