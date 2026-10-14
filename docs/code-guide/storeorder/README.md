# storeorder 코드 가이드

`src/main/java/com/kb/wms/storeorder/`의 패키지 구조를 따라 클래스별 설명을 모았다.

## 도메인 개요

> 이 도메인은 ERD의 **"발주"**로, **지점이 창고에 상품을 요청**하는 흐름이다. `inbound`의 `PurchaseOrder`(창고 → 공급처 발주)와 혼동하지 않는다.

```
점주 등록 ─▶ 본사 승인 ─▶ 본사 창고 배정 ─▶ 출고(할당·피킹·배송) ─▶ 완료
 REQUESTED    APPROVED      ASSIGNED   ↔ ON_HOLD (창고 관리자 보류·재개)        COMPLETED
     │                          │
     └ REJECTED(반려)           └ 부분 출고 종결(창고 관리자) ─▶ COMPLETED
     └ CANCELED(취소: 승인 전 점주, 승인 이후 본사)
```

출고·재고 할당은 `outbound` 도메인이 맡고, 이 도메인은 항목의 요청·할당·출고 **수량**과 발주 **상태**를 관리한다.

## 읽는 순서 추천

1. `domain/entity/StoreOrder.md`, `StoreOrderLine.md`, `domain/enums/Enums.md`
2. `application/service/StoreOrderService.md` : 전이별 규칙과 권한
3. `application/port/out/Ports.md` : 출고 연동 포트
4. `application/service/StoreOrderFulfillmentService.md` : 출고 → 발주 방향
5. `adapter/in/web/StoreOrderController.md`

## 문서 위치

| 패키지 | 문서 |
|---|---|
| `domain/entity` | StoreOrder, StoreOrderLine |
| `domain/enums` | `Enums.md` (상태 3종 + 진행 단계) |
| `exception` | `StoreOrderErrorCode.md` |
| `application/service` | StoreOrderService, StoreOrderFulfillmentService, StoreOrderPresenceService |
| `application/port/in` | `UseCases.md`, `command/Commands.md`, `query/SearchConditions.md`, `result/Results.md` |
| `application/port/out` | `Ports.md` |
| `adapter/in/web` | StoreOrderController, `dto/request/Requests.md`, `dto/response/Responses.md` |
| `adapter/out` | `Adapters.md`, `persistence/` 3개 문서 |

## 핵심 규칙

- **공급 단가 스냅샷**: 등록 시점의 SKU 공급 단가를 항목에 고정한다. 요청으로 받지 않는다
- **창고는 배정 전까지 null**: 목록 쿼리는 창고를 left join하고, 창고 관리자 범위 조회는 미배정 발주를 제외한다
- **전이마다 발주 행 비관적 락**: 동시 승인·반려·취소 중 하나만 성공
- **전이마다 상태 이력**: 사유와 처리자를 `StatusHistory`에 기록. 사유는 500자 이하
- **취소 권한**: 승인 전은 작성자 점주, 승인 이후는 본사. 피킹이 시작된 출고가 있으면 불가(`ORDER_IN_PICKING`)
- **승인 이후 취소**: 출고 쪽 READY 출고 취소와 재고 할당 해제를 같은 트랜잭션에서 처리
- **재배정**: ASSIGNED에서만(ON_HOLD 불가), 사유 필수, 재고 할당·출고가 남아 있으면 불가(`ORDER_IN_FULFILLMENT`)
- **부분 출고 종결**: 진행 중 출고가 없고 부족 수량이 있어야 한다. 남은 할당을 해제하며 자동 재발주하지 않는다
- **자동 완료**: 출고 쪽이 전량 출고를 확인하면 ASSIGNED → COMPLETED (`completeIfFulfilled`)
- **진행 단계(`progressStage`)는 파생 값**: 발주 상태 + 최근 출고 상태 + 부족 여부로 계산한다
- **순환 의존 회피**: 지점·발주·출고 도메인이 서로 호출하므로 UseCase를 셋으로 나눴다
