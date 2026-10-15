# inbound 코드 가이드

`src/main/java/com/kb/wms/inbound/`의 패키지 구조를 따라 클래스별 설명을 모았다.

## 도메인 개요

> 패키지 이름에 주의: `inbound` 패키지는 ERD의 **입고**에 해당하며 **공급처(Supplier)**, **창고 발주(PurchaseOrder)**, **입고(Inbound)**를 함께 담는다. 지점 → 창고 요청인 "발주(StoreOrder)"는 `storeorder` 패키지다.

```
Supplier ──▶ PurchaseOrder(창고 발주 요청) ──▶ SupplierOrder(공급처 발주서) ──▶ Inbound(도착·검수·완료) ──▶ 재고 반영
              REQUESTED→CONFIRMED→COMPLETED      ISSUED→PARTIALLY_RECEIVED→COMPLETED   ARRIVED→INSPECTING→COMPLETED
```

## 읽는 순서 추천

1. `domain/entity/` (PurchaseOrder → PurchaseOrderLine → Inbound → InboundLine) → `domain/enums/Enums.md`
2. `application/service/PurchaseOrderService.md` : 발주 등록·확정·취소 권한
3. `application/service/InboundService.md` → `InboundInspectService.md` → `InboundCompleteService.md` : 입고 흐름
4. `adapter/in/web/` → `application/port/out/Repositories.md` → `adapter/out/`

## 문서 위치

| 패키지 | 문서 |
|---|---|
| `domain/entity` | Inbound, InboundLine, PurchaseOrder, PurchaseOrderLine, Supplier |
| `domain/enums` | `Enums.md` |
| `exception` | `Exceptions.md` (오류 코드 3종 + 제약 대응표 3종) |
| `application/service` | InboundService, InboundInspectService, InboundCompleteService, PurchaseOrderService, SupplierOrderService, SkuSupplierService, SupplierService |
| `application/port/in` | `UseCases.md`, `command/Commands.md`, `query/SearchConditions.md`, `result/Results.md` |
| `application/port/out` | `Repositories.md` (영속성·조회·다른 도메인 연결 포트) |
| `adapter/in/web` | InboundController, PurchaseOrderController, SupplierOrderController, SkuSupplierController, SupplierController, `dto/request/Requests.md`, `dto/response/Responses.md` |
| `adapter/out` | `Adapters.md`(inventory·product·warehouse 연결), `persistence/` 3개 문서 |

## 핵심 규칙

- **발주 단가 스냅샷**: 발주 등록 시점의 SKU 매입 단가를 항목에 고정한다. 요청으로 받지 않는다
- **입고 창고 = 발주 창고**: 입고 요청에 창고가 없다
- **검수는 통째 교체**: 호출마다 검수 항목 전체를 교체하며, 재고·구역 사용량·발주 항목은 완료 전까지 바뀌지 않는다
- **로트 find-or-create**: 검수 시 (SKU, 공급처, 로트 번호)로 찾고 없으면 만든다(ADR-004)
- **완료는 한 트랜잭션**: 재고 반영, 구역 사용량, 재고 이력, 발주 항목 누적, 발주 자동 완료, 입고 완료
- **잠금 순서**: 입고 행 → 발주 행 → 발주 항목 행 → 구역 행 → 재고 행
- **완료 후 취소 불가**: 입고는 ARRIVED·INSPECTING에서만 취소(확정된 결정)
- **발주 취소 권한**: REQUESTED는 작성자 창고 관리자, CONFIRMED는 본사. 취소되지 않은 입고가 있으면 불가
- **공급처 발주서**: 확정 시 공급처당 한 건이 만들어지고(`SO-` 번호), 입고는 발주서에 연결된다. 완료는 라인 → 발주서 → 요청으로 전파된다(ADR-030)
- **부분 입고**: 같은 발주서에 입고를 여러 번 둘 수 있고, 한 번에 하나만 진행 중일 수 있다(`INBOUND_IN_PROGRESS`). 다른 발주서의 입고는 병렬이다
- **번호 채번**: `IB-`/`PO-` + 날짜 + 당일 개수 + 1. 동시 충돌은 유니크 제약이 409로 응답
- **상태 이력**: 입고·발주의 상태 전이와 취소 사유를 `StatusHistory`에 기록
