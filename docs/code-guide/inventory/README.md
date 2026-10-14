# inventory 코드 가이드

`src/main/java/com/kb/wms/inventory/`의 패키지 구조를 따라 클래스별 설명을 모았다.

## 도메인 개요

**구역 + 로트 단위의 재고 원장**, 로트 마스터, 수량 변동 이력을 관리한다. 입고·출고 도메인이 수량을 바꾸는 유일한 경로이며, 재고 조회·조정 API를 제공한다. 구조의 핵심은 **수량 정합성**(비관적 락, 고정된 잠금 순서, all-or-nothing)이다.

## 읽는 순서 추천

1. `domain/entity/InventoryLot.md` : 수량 규칙
2. `application/service/InventoryStockService.md` : 입고·할당·출고 반영, 잠금 순서
3. `application/service/InventoryAdjustmentService.md`, `LotService.md`
4. `application/service/InventoryQueryService.md`, `application/port/in/query/SearchConditions.md` : 담당 창고 범위
5. `adapter/in/web/` → `application/port/out/Repositories.md` → `adapter/out/`

## 문서 위치

| 패키지 | 문서 |
|---|---|
| `domain/entity` | InventoryLot, InventoryTransaction, Lot |
| `domain/enums` | `Enums.md` (QualityStatus, LotStatus, TransactionType, ReferenceType) |
| `exception` | `InventoryErrorCode.md` |
| `application/service` | InventoryStockService, InventoryAdjustmentService, InventoryQueryService, LotService |
| `application/port/in` | `UseCases.md`, `command/Commands.md`, `query/SearchConditions.md`, `result/Results.md` |
| `application/port/out` | `Repositories.md` (Repository 4종 + SectionCapacityPort, SkuStatusPort) |
| `adapter/in/web` | InventoryController, LotController, `dto/request/Requests.md`, `dto/response/Responses.md` |
| `adapter/out` | `Adapters.md`(product·warehouse 연결), `persistence/` 3개 문서 |

## 핵심 규칙

- **수량 규칙**: `0 <= allocated <= onHand`, 가용 = `onHand - allocated`. 가용 판단은 품질·로트 상태가 모두 AVAILABLE일 때만
- **잠금 순서**: 구역 행(section_id 오름차순) → 재고 행(inventory_lot_id 오름차순). 입고·출고·조정이 모두 같은 순서를 지킨다(ADR-006)
- **이력**: 보유 수량이 바뀌는 입고·출고·조정만 `InventoryTransaction`을 남기고, 할당·해제는 남기지 않는다
- **FEFO 할당 후보**: 유통기한이 빠른 로트부터 (`findAllocatableStocks`)
- **로트 find-or-create**: (SKU, 공급처, 로트 번호)가 같으면 재사용, 원가·일자가 다르면 거절(ADR-004)
- **재고 조정 중복 방지**: `beforeQuantity`가 서버 값과 같을 때만 반영(`STALE_QUANTITY`)
- **로트 상태 변경**은 본사만. 격리는 할당이 없어야, 폐기는 수량이 없어야 가능하며 상태 이력에 기록
- **담당 창고 범위**: 창고 관리자는 담당 창고의 재고만 본다. 범위 밖이면 403
