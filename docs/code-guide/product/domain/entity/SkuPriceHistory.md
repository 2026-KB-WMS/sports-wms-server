# SkuPriceHistory

`product/domain/entity/SkuPriceHistory.java`

SKU 현재 단가(매입·공급)가 바뀐 기록. 단가가 바뀐 수정 한 번당 한 건이며, 바꾸지 않은 쪽 단가도 이전·이후가 같은 값으로 들어간다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| skuPriceHistoryId | Long | PK |
| skuId | Long | SKU ID |
| previousPurchasePrice / newPurchasePrice | BigDecimal | 변경 전·후 매입 단가 |
| previousSupplyPrice / newSupplyPrice | BigDecimal | 변경 전·후 공급 단가 |
| changedBy | Long | 변경한 사용자 ID |
| changedAt | LocalDateTime | 변경 시각 (`record` 호출 시점) |

## 메서드

| 메서드 | 설명 |
|---|---|
| `record(skuId, prevPurchase, newPurchase, prevSupply, newSupply, changedBy)` | 이력 한 건 생성. skuId 또는 changedBy가 null이면 `IllegalArgumentException` |
