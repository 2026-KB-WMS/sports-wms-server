# 조회 결과 (Result)

`inbound/application/port/in/result/` · 대부분 조회 쿼리가 JPQL 생성자 표현식으로 직접 만드는 읽기 전용 `record`다. 발주·공급처·창고·SKU·로트·구역·사용자 이름을 ID로 조인해 채운다(ADR-007).

| 클래스 | 용도 | 엔드포인트 | 만드는 곳 | 쓰는 곳 |
|---|---|---|---|---|
| `InboundSummary` | 입고 목록 항목 | GET `/inbounds` | `InboundJpaRepository.search` | `InboundSummaryResponse` |
| `InboundView` | 입고 헤더 한 건 + 발주·공급처·창고 정보 | GET `/inbounds/{id}`, 등록·취소 응답 | `InboundJpaRepository.findView` + 서비스가 취소 사유 보강 | `InboundResponse`, `InboundRegisterResponse`, `InboundCancelResponse` |
| `InboundDetails` | 입고 식별 정보 + 검수 항목 | GET `/inbounds/{id}/details`, 검수 응답 | `InboundService.getInboundDetails` | `InboundDetailsResponse`, `InboundInspectResponse` |
| `InboundLineView` | 검수 항목 한 줄 | 위와 같음 | `InboundLineJpaRepository.findLineViews` | `InboundLineResponse` |
| `InboundCompleteResult` | 입고 완료 결과 | PATCH `/inbounds/{id}/complete` | `InboundCompleteService` | `InboundCompleteResponse` |
| `SectionCandidate` | 검수 구역 후보 | GET `.../assignable-sections`, `.../defect-sections` | `InboundJpaRepository.findAssignableSections`, `findDefectSections` | `SectionCandidatesResponse` |
| `PurchaseOrderSummary` | 발주 목록 항목 | GET `/purchase-orders` | `PurchaseOrderJpaRepository.search` | `PurchaseOrderSummaryResponse` |
| `PurchaseOrderView` | 발주 헤더 한 건 | GET `/purchase-orders/{id}`, 등록 응답 | `findView` + 서비스가 취소 사유 보강 | `PurchaseOrderResponse`, `PurchaseOrderRegisterResponse` |
| `PurchaseOrderDetails` | 발주 식별 정보 + SKU별 항목 | GET `.../details` | `PurchaseOrderService.getPurchaseOrderDetails` | `PurchaseOrderDetailsResponse` |
| `PurchaseOrderLineView` | 발주 항목 한 줄 | 위와 같음 | `PurchaseOrderLineJpaRepository.findLineViews` | `PurchaseOrderLineResponse` |

## 취소 사유 보강 패턴

`InboundView`, `PurchaseOrderView`는 `cancelReason`을 조회 쿼리에서 읽지 않는다. 쿼리용 보조 생성자는 `cancelReason = null`로 만들고, 서비스가 **CANCELED 상태일 때만** 상태 이력(`StatusHistory`)에서 취소 사유를 읽어 `withCancelReason(...)`으로 채운다. 사유 없이 취소했으면 null이다.

## 필드 참고

| 클래스 | 필드 설명 |
|---|---|
| `InboundSummary` | `lineCount`는 검수 항목 수(ARRIVED는 0). `receivedByName`은 검수 전·취소된 입고에서 null |
| `InboundView` | `purchaseOrderStatus`는 취소·완료 응답에서 발주 상태를 함께 내려주려는 값 |
| `InboundLineView` | `orderedUnitPrice`는 발주 항목의 단가 스냅샷. 구역이 미정이면 구역 ID·코드는 null |
| `InboundCompleteResult` | `inventory`(항목별 `ReflectedInventory`: 합격·불량 재고 행 ID와 수량), `purchaseOrderLines`(`PurchaseOrderLineProgress`: 누적 수량·상태) |
| `PurchaseOrderSummary/View` | `lineCount`(항목 수), `totalAmount`(항목 금액 합계) |
| `PurchaseOrderLineView` | `remainingQuantity`는 `max(0, expected - received)` |
| `SectionCandidate` | `availableCapacity = capacity - currentCapacity`, 후보는 항상 0보다 크다 |
