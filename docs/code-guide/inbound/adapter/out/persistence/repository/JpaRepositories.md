# Spring Data JPA 리포지토리

`inbound/adapter/out/persistence/repository/`

| 리포지토리 | 엔티티 | 직접 정의한 메서드 |
|---|---|---|
| `InboundJpaRepository` | `InboundJpaEntity` | `existsByInboundNo`, `countByInboundNoStartingWith`, `findByIdForUpdate`, `existsInProgressByPurchaseOrderId`, `existsInProgressBySupplierOrderId`, `existsNotCanceledByPurchaseOrderId`, `search`, `findView`, `findAssignableSections`, `findDefectSections` |
| `InboundLineJpaRepository` | `InboundLineJpaEntity` | `findByInboundIdOrderByInboundLineIdAsc`, `deleteByInboundId`, `findLineViews` |
| `PurchaseOrderJpaRepository` | `PurchaseOrderJpaEntity` | `existsByPurchaseOrderNo`, `countByPurchaseOrderNoStartingWith`, `findByIdForUpdate`, `search`, `findView` |
| `PurchaseOrderLineJpaRepository` | `PurchaseOrderLineJpaEntity` | `findByPurchaseOrderIdOrderByPurchaseOrderLineIdAsc`, `findByPurchaseOrderIdForUpdate`, `findLineViews` |
| `SupplierJpaRepository` | `SupplierJpaEntity` | `existsBySupplierCode`, `search` |

## 주요 쿼리

| 메서드 | 설명 |
|---|---|
| `InboundJpaRepository.search` | 입고에 발주(번호·공급처)·창고·처리자명을 붙이고 **검수 항목 수를 `count`**한다. 검수 전(ARRIVED)에는 항목이 없으므로 항목은 **left join**. `group by`로 집계. 도착 일시 내림차순 |
| `InboundJpaRepository.findView` | 위와 같은 조인으로 한 건 |
| `InboundJpaRepository.findAssignableSections` | 창고의 구역 중 `ACTIVE`, `sectionType <> 'DEFECT'`, `capacity - currentCapacity > 0`(요청 수량이 있으면 그 이상). 구역 코드순 |
| `InboundJpaRepository.findDefectSections` | 위와 같되 `sectionType = 'DEFECT'` |
| `InboundJpaRepository.existsInProgressByPurchaseOrderId` | 상태가 ARRIVED·INSPECTING |
| `InboundJpaRepository.existsNotCanceledByPurchaseOrderId` | 상태가 CANCELED가 아님 |
| `InboundLineJpaRepository.deleteByInboundId` | `@Modifying(flushAutomatically, clearAutomatically)` 벌크 삭제. 검수 항목 교체 시 새 항목과 유니크 키가 겹치지 않도록 **즉시 삭제하고 영속성 컨텍스트를 비운다** |
| `InboundLineJpaRepository.findLineViews` | 발주 항목(단가 스냅샷·SKU), 로트, 구역(left join), 처리자명을 붙여 조회. SKU 코드·로트 번호순 |
| `PurchaseOrderJpaRepository.search` | 창고·공급처를 붙이고 항목과 **inner join**해 항목 수·금액 합계 집계(발주는 항목이 1개 이상). 등록 일시 내림차순 |
| `InboundJpaRepository.existsInProgressBySupplierOrderId` | 상태가 ARRIVED·INSPECTING |
| `SupplierOrderJpaRepository.existsInProgressBySupplierId` | 상태가 ISSUED·PARTIALLY_RECEIVED |
| `PurchaseOrderLineJpaRepository.findByPurchaseOrderIdForUpdate` | `PESSIMISTIC_WRITE`, 항목 ID 오름차순 |
| `PurchaseOrderLineJpaRepository.findLineViews` | SKU 정보를 붙이고 `remainingQuantity`를 `case`로 계산 (SKU 코드순) |
| `SupplierJpaRepository.search` | keyword가 공급처명·코드·담당자명에 매칭. 공급처명, ID 오름차순 |
