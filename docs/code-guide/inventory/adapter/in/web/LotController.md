# LotController

`inventory/adapter/in/web/LotController.java` · `@RestController` · Swagger 태그 "로트"

로트는 **별도 생성 API가 없고** 입고 검수 트랜잭션 안에서 만들어진다(ADR-004).

## 의존

`LotUseCase`, `InventoryQueryUseCase`, `WarehouseUseCase`(창고명 조회)

## 엔드포인트

| 메서드 | 경로 | 상태 | 쿼리·바디 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|---|
| `getLots` | GET `/api/v1/lots` | 200 | `skuId`, `supplierId`, `expiringBefore`(날짜), `keyword` | `ItemsResponse<LotSummaryResponse>` | 로트 목록 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getLot` | GET `/api/v1/lots/{lotId}` | 200 | - | `LotDetailResponse` | 로트 상세 + 구역별 재고 + 입고 이력 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `changeLotStatus` | PATCH `/api/v1/lots/{lotId}/status` | 200 | `LotStatusChangeRequest` | `LotSummaryResponse` | 상태 변경 | **HQ_ADMIN** |

## getLot 조립

한 번의 응답에 세 가지를 합친다.

1. 로트 요약: `lotUseCase.getLot`
2. 구역별 재고: `inventoryQueryUseCase.getInventoriesByLot(InventoryLotSearchCondition.ofLot(lotId), principal)`. 창고명은 창고당 한 번만 조회한다(`warehouseNames` 맵)
3. 입고 이력: `lotUseCase.getLotInbounds`

## changeLotStatus

변경 후 `lotUseCase.getLot`을 다시 조회해 응답한다. 처리자는 토큰 주체다. 상태 변경 규칙은 `LotService` 문서를 본다.
