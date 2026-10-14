# outbound 코드 가이드

`src/main/java/com/kb/wms/outbound/`의 패키지 구조를 따라 클래스별 설명을 모았다.

## 도메인 개요

**지점 발주를 창고에서 실제로 내보내는 흐름**을 맡는다. 재고를 **예약(할당)**하고, 할당을 **출고로 묶고**, 피킹·배송을 거쳐 완료한다. 재고 수량 변경은 재고 도메인에, 발주 수량·상태 변경은 지점 발주 도메인에 맡긴다.

```
발주 ASSIGNED
 ① 재고 할당  ALLOCATED   ─ 해제 ─▶ RELEASED
 ② 출고 생성  READY       ─ 취소(READY만) ─▶ CANCELED
 ③ 피킹 시작  PICKING
 ④ 피킹 완료  PICKED      (재고 차감, 할당 PICKED, 발주 항목 출고 수량 증가)
 ⑤ 배송 시작  SHIPPED
 ⑥ 배송 완료  DELIVERED   (전량 출고 + 진행 중 출고 없음이면 발주 COMPLETED)
```

## 읽는 순서 추천

1. `domain/entity/` (StockAllocation → Outbound → OutboundLine), `domain/enums/Enums.md`
2. `application/port/in/UseCases.md` : 업무 순서와 서비스 A·B·C
3. `application/service/StockAllocationService.md` → `OutboundService.md` → `OutboundFulfillmentService.md`
4. `application/service/OutboundOrderLinkService.md` : 발주 도메인에 제공하는 출고·할당 조회와 정리
   `adapter/out/Adapters.md` : 발주 도메인의 서비스를 부르는 어댑터
5. `adapter/in/web/`, `adapter/out/persistence/`

## 문서 위치

| 패키지 | 문서 |
|---|---|
| `domain/entity` | StockAllocation, Outbound, OutboundLine |
| `domain/enums` | `Enums.md` (OutboundStatus, AllocationStatus) |
| `exception` | `OutboundErrorCode.md` |
| `application/service` | StockAllocationService(A), OutboundService(B), OutboundFulfillmentService(C) |
| `application/port/in` | `UseCases.md`, `command/Commands.md`, `query/SearchConditions.md`, `result/Results.md` |
| `application/port/out` | `Ports.md` |
| `adapter/in/web` | AllocationController, OutboundController, `dto/request/Requests.md`, `dto/response/Responses.md` |
| `adapter/out` | `Adapters.md`, `persistence/` 3개 문서 |

## 핵심 규칙

- **FEFO 할당**: 유통기한이 빠른 로트부터 예약한다. 한 발주 항목이 여러 재고 행에 나뉠 수 있다
- **부분 할당**: 가용분만큼 할당하고 부족분을 `shortages`로 돌려준다. 하나도 못 채울 때만 거절한다
- **출고는 할당을 묶는 단위**: 생성 때는 재고·할당·발주 수량이 바뀌지 않는다. 한 발주에 출고를 여러 건 만들 수 있다
- **취소는 READY만**: 피킹이 시작되면 할당 해제도 발주 취소도 막힌다
- **피킹 완료가 재고 차감 시점**: 부족분(할당 - 피킹)의 예약은 풀려 다시 가용 재고가 되고, 발주 항목의 미출고 수량으로 남아 다시 할당할 수 있다
- **확정 공급 단가**: 발주 항목 스냅샷 → SKU 현재 단가 → `SUPPLY_PRICE_MISSING`
- **발주 자동 완료**: 배송 완료 때 전량 출고이고 진행 중 출고가 없으면 발주를 COMPLETED로 전환
- **잠금 순서**: 발주 헤더 → 출고 헤더 → 할당 → 발주 항목 → (재고) 구역 → 재고 행. 전역 락 순서는 성능 개선 단계 과제
- **창고 기준**: 출고·할당은 창고를 직접 갖지 않고 **발주에 배정된 창고**로 담당 창고를 판단한다
- **상태 이력**: 할당(`STOCK_ALLOCATION`)과 출고(`OUTBOUND`)의 전이를 `StatusHistory`에 기록
